/*
 * Copyright (c) 2006-2007 Shane Mc Cormack
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * SVN: $Id$
 */
package uk.org.dataforce.dfbnc;

import uk.org.dataforce.logger.Logger;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.commands.CommandNotFound;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.util.Hashtable;
import com.dmdirc.parser.IRCParser;

/**
 * This socket handles actual clients connected to the bnc.
 */
public class UserSocket extends ConnectedSocket {
	/** Known sockets are referenced in this hashtable. */
	private static Hashtable<String,UserSocket> knownSockets = new Hashtable<String,UserSocket>();
	
	/** This sockets ID in the hashtable. */
	private final String myID;

	/** This sockets info. */
	private final String myInfo;
	
	/**
	 * This is true if a 001 has been sent to the user.
	 * Before 001 NOTICE AUTH should be used for messages rather than
	 * NOTICE/PRIVMSG/SNOTICE
	 */
	private boolean post001 = false;
	
	/** Given username */
	private String username = null;
	/** Given realname */
	private String realname = null;
	/** Given nickname (post-authentication this is the nickname the client knows itself as) */
	private String nickname = null;
	/** Given password */
	private String password = null;
	
	/** The Account object for this connect (This is null before authentication) */
	private Account myAccount = null;
	
	/**
	 * Create a new UserSocket.
	 *
	 * @param sChannel Socket to control
	 * @param threadName Name to call thread that this socket runs under.
	 */
	public UserSocket(SocketChannel sChannel) throws IOException {
		super(sChannel, "[UserSocket "+sChannel+"]");
		synchronized (knownSockets) {
			String tempid = this.toString();
			while (knownSockets.containsKey(tempid)) {
				tempid = this.toString()+"-"+Math.random();
			}
			myID = tempid;
			knownSockets.put(myID, this);
		}
		
		myThread.setName("[UserSocket: "+myID+"]");
		
		// myInfo = mySocket.getRemoteSocketAddress()+" ("+mySocket.getLocalSocketAddress()+") ["+myID+"]";
		// Do this rather than above because we want to enclose the addresses in []
		InetSocketAddress address = (InetSocketAddress)mySocket.getRemoteSocketAddress();
		String remoteInfo = "["+address.getAddress()+"]:"+address.getPort();
		address = (InetSocketAddress)mySocket.getLocalSocketAddress();
		String localInfo = "["+address.getAddress()+"]:"+address.getPort();
		myInfo = remoteInfo+" ("+localInfo+") ["+myID+"]";
		
		Logger.info("User Connected: "+myInfo);
	}
	
	/**
	 * Close all usersockets
	 *
	 * @param reason Reason for all sockets to close.
	 */
	public static void closeAll(final String reason) {
		for (String name : knownSockets.keySet()) {
			UserSocket socket = knownSockets.get(name);
			socket.sendLine(":%s NOTICE :Connection terminating (%s)", Functions.getServerName(), reason);
		}
	}
	
	/**
	 * Action to take when socket is opened and ready.
	 */
	public void socketOpened() {
		sendBotMessage("Welcome to DFBnc ("+DFBnc.VERSION+")");
	}
	
	/**
	 * Get the account linked to this socket
	 *
	 * @return Account object that is associated with this socket
	 */
	public Account getAccount() { return myAccount; }
	
	/**
	 * Send a message to the user from the bnc bot in printf format.
	 *
	 * @param data The format string
	 * @param args The args for the format string
	 */
	public void sendBotMessage(final String data, final Object... args) {
		if (post001) {
			if (myAccount != null) {
				final String method = myAccount.getContactMethod();
				if (method.equalsIgnoreCase("SNOTICE")) {
					sendServerLine("NOTICE", data, args);
				} else {
					sendBotLine(method, data, args);
				}
			} else {
				sendServerLine("NOTICE", data, args);
			}
		} else {
			sendLine("NOTICE AUTH :- "+String.format(data, args));
		}
	}
	

	/**
	 * Send a message to the user from the bnc bot in printf format.
	 *
	 * @param type the Type of message to send
	 * @param data The format string
	 * @param args The args for the format string
	 */
	public void sendBotLine(final String type, final String data, final Object... args) {
		sendLine(":%s!bot@%s %s %s :%s", Functions.getBotName(), Functions.getServerName(), type, nickname, String.format(data, args));
	}
	
	/**
	 * Send a message to the user from the bnc server in printf format.
	 *
	 * @param type the Type of message to send
	 * @param data The format string
	 * @param args The args for the format string
	 */
	public void sendServerLine(final String type, final String data, final Object... args) {
		sendLine(":%s %s %s :%s", Functions.getServerName(), type, nickname, String.format(data, args));
	}
	
	/**
	 * Action to take when socket is closed.
	 *
	 * @param userRequested True if socket was closed by the user, false otherwise
	 */
	protected void socketClosed(final boolean userRequested) {
		knownSockets.remove(myID);
		Logger.info("User Disconnected: "+myInfo);
	}
	
	/**
	 * Check if there is enough parameters, if not, return an error.
	 *
	 * @param newLine the Parameters String
	 * @param count the number of parameters required
	 * @return True if there is enough parameters, else false
	 */
	private boolean checkParamCount(final String[] newLine, final int count) {
		if (newLine.length < count) {
			sendIRCLine(Consts.ERR_NEEDMOREPARAMS, newLine[0], "Not enough parameters");
			return false;
		}
		return true;
	}
	
	/**
	 * Process a line of data.
	 *
	 * @param line Line to handle
	 */
	protected void processLine(final String line) {
		// Tokenise the line
		final String[] newLine = IRCParser.tokeniseLine(line);
		
		if (!checkParamCount(newLine, 1)) { return; }
		
		newLine[0] = newLine[0].toUpperCase();
		
		// Pass it on the appropriate processing function
		if (newLine[0].equals("QUIT")) {
			sendLine("Closing Connection");
			close();
		} else if (myAccount != null) {
			processAuthenticated(newLine);
		} else {
			processNonAuthenticated(newLine);
		}
	}
	
	/**
	 * Process a line of data from a non-authenticated user.
	 *
	 * @param line IRCTokenised version of Line to handle
	 */
	private void processNonAuthenticated(final String[] line) {
		if (!checkParamCount(line, 2)) { return; }
		if (line[0].equals("USER")) {
			// Username may be given in PASS so check that it hasn't before assigning
			if (username == null) { username = line[1]; }
			realname = line[line.length-1];
			if (nickname != null && password == null) {
				sendBotMessage("Please enter your password.");
				sendBotMessage("This can be done using either: ");
				sendBotMessage("    /QUOTE PASS [<username>:]<password");
				sendBotMessage("    /RAW PASS [<username>:]<password>");
			}
		} else if (line[0].equals("NICK")) {
			nickname = line[1];
			if (realname != null && password == null) {
				sendBotMessage("Please enter your password.");
				sendBotMessage("This can be done using either: ");
				sendBotMessage("    /QUOTE PASS [<username>:]<password");
				sendBotMessage("    /RAW PASS [<username>:]<password>");
			}
		} else if (line[0].equals("PASS")) {
			String[] bits = line[line.length-1].split(":",2);
			if (bits.length == 2) {
				username = bits[0];
				password = bits[1];
			} else {
				password = bits[0];
			}
		} else {
			sendIRCLine(Consts.ERR_NOTREGISTERED, line[0], "You must login first.");
		}
		
		if (realname != null && password != null && nickname != null) {
			if (Account.count() == 0) {
				Account acc = Account.createAccount(username, password);
				acc.setAdmin(true);
				sendBotMessage("You are the first user of this bnc, and have been made admin");
				Config.saveAll(DFBnc.getConfigFileName());
			}
			if (Account.checkPassword(username, password)) {
				myAccount = Account.get(username);
				sendBotMessage("You are now logged in");
				if (myAccount.isAdmin()) {
					sendBotMessage("This is an Admin account");
				}
				// Run the firsttime command if this is the first time the account has been used
				if (myAccount.isFirst()) {
					handleBotCommand(new String[]{"firsttime"});
					if (myAccount.isAdmin()) {
						sendBotMessage("");
						handleBotCommand(new String[]{"firsttime", "admin"});
					}
				}
			} else {
				sendIRCLine(Consts.ERR_PASSWDMISMATCH, line[0], "Password incorrect, or account not found");
				close();
			}
		}
	}
	
	/**
	 * Process a line of data from an authenticated user.
	 *
	 * @param line IRCTokenised version of Line to handle
	 */
	private void processAuthenticated(final String[] line) {
		// The bnc accepts commands as either:
		// /msg -BNC This is a command
		// or /DFBNC This is a command (not there is no : used to separate arguments anywhere)
		if (line[0].equals("PRIVMSG") && line.length > 2) {
			if (line[1].equalsIgnoreCase(Functions.getBotName())) {
				handleBotCommand(line[2].split(" "));
				return;
			}
		} else if (line[0].equals("DFBNC") && line.length > 1) {
			String[] bits = new String[line.length-1];
			System.arraycopy(line, 1, bits, 0, line.length-1);
			handleBotCommand(bits);
			return;
		}
		
		// We don't handle this ourselves, send it to the server.
	}
	
	/**
	 * Handle a command sent to the bot
	 *
	 * @param bits This is the command and its parameters.
	 *             bits[0] is the command, bits[1]..bits[n] are the params.
	 */
	private void handleBotCommand(final String[] bits) {
		try {
			if (myAccount != null) {
				myAccount.getCommandManager().handle(this, bits);
			}
		} catch (CommandNotFound c) {
			sendBotMessage("Unknown command '%s' Please try 'ShowCommands'", bits[0]);
		} catch (Exception e) {
			sendBotMessage("Exception with command '%s': %s", bits[0], e.getMessage());
		}
	}
}

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

import uk.org.dataforce.libs.logger.Logger;
import uk.org.dataforce.dfbnc.commands.CommandNotFoundException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.util.HashMap;
import com.dmdirc.parser.irc.IRCParser;
import java.util.ArrayList;
import java.util.List;

/**
 * This socket handles actual clients connected to the bnc.
 */
public class UserSocket extends ConnectedSocket {
	/** Known sockets are referenced in this HashMap. */
	private static HashMap<String,UserSocket> knownSockets = new HashMap<String,UserSocket>();
	
	/** This sockets ID in the HashMap. */
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
	
	/** IP Address of this socket */
	private String myIP = "0.0.0.0";
	
	/** The Account object for this connect (This is null before authentication) */
	private Account myAccount = null;
	
	/** Is closeAll being run? (This prevents socketClosed removing the HashMap entry) */
	private static boolean closeAll = false;
	
	/**
	 * Create a new UserSocket.
	 *
	 * @param sChannel Socket to control
	 * @param fromSSL Did this come from an SSL ListenSocket ?
	 * @throws IOException If there is a problem setting up the socket.
	 */
	public UserSocket(SocketChannel sChannel, final boolean fromSSL) throws IOException {
		super(sChannel, "[UserSocket "+sChannel+"]", fromSSL);
		synchronized (knownSockets) {
			String tempid = this.toString();
			while (knownSockets.containsKey(tempid)) {
				tempid = this.toString()+"-"+Math.random();
			}
			myID = tempid;
			knownSockets.put(myID, this);
		}
		
		super.setSocketID("[UserSocket: "+myID+"]");
		
		// myInfo = mySocket.getRemoteSocketAddress()+" ("+mySocket.getLocalSocketAddress()+") ["+myID+"]";
		// Do this rather than above because we want to enclose the addresses in []
		InetSocketAddress address = (InetSocketAddress)mySocketWrapper.getRemoteSocketAddress();
		String remoteInfo = "["+address.getAddress()+"]:"+address.getPort();
		address = (InetSocketAddress)mySocketWrapper.getLocalSocketAddress();
		String localInfo = "["+address.getAddress()+"]:"+address.getPort();
		if (fromSSL) {
			myInfo = remoteInfo+" ("+localInfo+" [SSL]) ["+myID+"]";
		} else {
			myInfo = remoteInfo+" ("+localInfo+") ["+myID+"]";
		}
		
		myIP = address.getAddress().getHostAddress();
		Logger.info("User Connected: "+myInfo);
	}
	
	/**
	 * Get the IP address of this socket
	 *
	 * @return IP Address of this socket.
	 */
	public String getIP() {
		if (isSSL) {
			return '@'+myIP;
		} else {
			return myIP;
		}
	}
	
	/**
	 * Get a List of all UserSockets that are part of a given account
	 *
	 * @param account Account to check sockets against
	 * @return a Collection of all UserSockets that are part of the given account
	 */
	public static List<UserSocket> getUserSockets(final Account account) {
		ArrayList<UserSocket> list = new ArrayList<UserSocket>();
		synchronized (knownSockets) {
			for (UserSocket socket : knownSockets.values()) {
				if (socket.getAccount() == account) {
					list.add(socket);
				}
			}
		}
		return list;
	}
	
	/**
	 * Close all usersockets
	 *
	 * @param reason Reason for all sockets to close.
	 */
	public static void closeAll(final String reason) {
		closeAll = true;
		synchronized (knownSockets) {
			for (UserSocket socket : knownSockets.values()) {
				socket.sendLine(":%s NOTICE :Connection terminating (%s)", Functions.getServerName(socket.getAccount()), reason);
				socket.close();
			}
		}
		closeAll = false;
	}
	
	/**
	 * Action to take when socket is opened and ready.
	 */
	@Override
	public void socketOpened() {
		sendBotMessage("Welcome to DFBnc ("+DFBnc.VERSION+")");
		if (isSSL) {
			sendBotMessage("You are connected using SSL");
		} else {
			sendBotMessage("You are not connected using SSL");
		}
	}
	
	/**
	 * Get the account linked to this socket
	 *
	 * @return Account object that is associated with this socket
	 */
	public Account getAccount() { return myAccount; }
	
	/**
	 * Get the realname supplied to this socket
	 *
	 * @return Realname supplied to this socket
	 */
	public String getRealname() { return realname; }
	
	/**
	 * Get the nickname for this socket
	 *
	 * @return nickname for this socket
	 */
	public String getNickname() { return nickname; }
	
	/**
	 * Set the nickname for this socket
	 *
	 * @return nickname for this socket
	 */
	public void setNickname(String newValue) { nickname = newValue; }
	
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
	 * Get the status of post001
	 *
	 * @return True if this socket has had a 001 sent to it, else false
	 */
	public boolean getPost001() { return post001; }
	
	/**
	 * Get the status of post001
	 *
	 * @param newValue new value for post001, True if this socket has had a 001 sent to it, else false
	 */
	public void setPost001(final boolean newValue) { post001 = newValue; }
	
	/**
	 * Send a given raw line to all sockets
	 *
	 * @param line Line to send
	 * @param ignoreThis Don't send the line to this socket if true
	 */
	public void sendAll(final String line, final boolean ignoreThis) {
		for (UserSocket socket : this.getAccount().getUserSockets()) {
			if (ignoreThis && socket == this) { continue; }
			socket.sendLine(line);
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
		sendLine(":%s!bot@%s %s %s :%s", Functions.getBotName(), Functions.getServerName(myAccount), type, nickname, String.format(data, args));
	}
	
	/**
	 * Send a message to the user from the bnc server in printf format.
	 *
	 * @param type the Type of message to send
	 * @param data The format string
	 * @param args The args for the format string
	 */
	public void sendServerLine(final String type, final String data, final Object... args) {
		sendLine(":%s %s %s :%s", Functions.getServerName(myAccount), type, nickname, String.format(data, args));
	}
	
	/**
	 * Action to take when socket is closed.
	 *
	 * @param userRequested True if socket was closed by the user, false otherwise
	 */
	@Override
	protected void socketClosed(final boolean userRequested) {
		if (!closeAll) {
			synchronized (knownSockets) {
				knownSockets.remove(myID);
			}
		}
		Logger.info("User Disconnected: "+myInfo);
		if (myAccount != null) {
			myAccount.userDisconnected(this);
		}
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
	@Override
	protected void processLine(final String line) {
		// Tokenise the line
		final String[] newLine = IRCParser.tokeniseLine(line);
		
		if (!checkParamCount(newLine, 1)) { return; }
		
		newLine[0] = newLine[0].toUpperCase();
		
		// Pass it on the appropriate processing function
		if (newLine[0].equals("QUIT")) {
			close();
		} else if (myAccount != null) {
			processAuthenticated(line, newLine);
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
				sendBotMessage("    /QUOTE PASS [<username>:]<password>");
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
			if (Account.count() == 0 || (Config.getBoolOption("debugging", "autocreate", false) && !Account.exists(username))) {
				Account acc = Account.createAccount(username, password);
				if (Account.count() == 1) {
					acc.setAdmin(true);
					sendBotMessage("You are the first user of this bnc, and have been made admin");
				} else {
					sendBotMessage("The given account does not exist, so an account has been created for you.");
				}
				Config.saveAll(DFBnc.getConfigFileName());
			}
			if (Account.checkPassword(username, password)) {
				myAccount = Account.get(username);
				if (myAccount.isSuspended()) {
					sendBotMessage("This account has been suspended.");
					sendBotMessage("Reason: "+myAccount.getSuspendReason());
					myAccount = null;
					close();
				} else {
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
					myAccount.userConnected(this);
				}
			} else {
				sendIRCLine(Consts.ERR_PASSWDMISMATCH, line[0], "Password incorrect, or account not found");
				close();
			}
		}
	}
	
	/**
	 * Used to send a line of data to this socket, for an irc response
	 *
	 * @param numeric The numeric for this line
	 * @param params The parameters for this line
	 * @param line Information
	 */
	public final void sendIRCLine(final int numeric, final String params, final String line) {
		sendIRCLine(numeric, params, line, true);
	}
	
	/**
	 * Used to send a line of data to this socket, for an irc response
	 *
	 * @param numeric The numeric for this line
	 * @param params The parameters for this line
	 * @param line Information
	 * @param addColon Automatically add : before line
	 */
	public final void sendIRCLine(final int numeric, final String params, final String line, final boolean addColon) {
		if (addColon) {
			sendLine(":%s %03d %s :%s", Functions.getServerName(myAccount), numeric, params, line);
		} else {
			sendLine(":%s %03d %s %s", Functions.getServerName(myAccount), numeric, params, line);
		}
	}
	
	/**
	 * Process a line of data from an authenticated user.
	 *
	 * @param normalLine Non-IRCTokenised version of Line to handle
	 * @param line IRCTokenised version of Line to handle
	 */
	private void processAuthenticated(final String normalLine, final String[] line) {
		// The bnc accepts commands as either:
		// /msg -BNC This is a command
		// or /DFBNC This is a command (not there is no : used to separate arguments anywhere)
		if ((line[0].equalsIgnoreCase("PRIVMSG") || line[0].equalsIgnoreCase("NOTICE")) && line.length > 2) {
			if (line[1].equalsIgnoreCase(Functions.getBotName())) {
				handleBotCommand(line[2].split(" "));
				return;
			} else {
				final String myHost = this.getAccount().getConnectionHandler().getMyHost();
				if (myHost != null) {
					sendAll(String.format("%s %s", myHost, normalLine), true);
				}
			}
		} else if (line[0].equalsIgnoreCase("DFBNC") && line.length > 1) {
			String[] lineBits = normalLine.split(" ");
			String[] bits = new String[lineBits.length-1];
			System.arraycopy(lineBits, 1, bits, 0, lineBits.length-1);
			handleBotCommand(bits);
			return;
		} else if (line[0].equalsIgnoreCase("PING")) {
			if (line.length > 1) {
				sendLine(":%s PONG %1$s %s", Functions.getServerName(myAccount), line[1]);
			} else {
				sendLine(":%s PONG %1$s %s", Functions.getServerName(myAccount), System.currentTimeMillis());
			}
		} else if (line[0].equalsIgnoreCase("WHOIS")) {
			if (line[1].equalsIgnoreCase(Functions.getBotName())) {
				sendIRCLine(Consts.RPL_WHOISUSER, nickname+" "+Functions.getBotName()+" bot "+Functions.getServerName(myAccount)+" *", "DFBnc Pseudo Client");
				sendIRCLine(Consts.RPL_WHOISSERVER, nickname+" "+Functions.getBotName()+" DFBNC.Server", "DFBnc Pseudo Server");
				sendIRCLine(Consts.RPL_WHOISIDLE, nickname+" "+Functions.getBotName()+" 0 "+(DFBnc.getStartTime()/1000), "seconds idle, signon time");
				sendIRCLine(Consts.RPL_ENDOFWHOIS, nickname+" "+Functions.getBotName(), "End of /WHOIS list");
				return;
			}
		}
		
		
		
		// We don't handle this ourselves, send it to the ConnectionHandler
		ConnectionHandler myConnectionHandler = myAccount.getConnectionHandler();
		if (myConnectionHandler != null) {
			myConnectionHandler.dataRecieved(this, normalLine, line);
		} else {
			sendIRCLine(Consts.ERR_UNKNOWNCOMMAND, line[0], "Unknown command");
		}
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
		} catch (CommandNotFoundException c) {
			sendBotMessage("Unknown command '%s' Please try 'ShowCommands'", bits[0]);
		} catch (Exception e) {
			sendBotMessage("Exception with command '%s': %s", bits[0], e.getMessage());
		}
	}
}

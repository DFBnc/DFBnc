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
	
	/** Given username */
	private String username = null;
	/** Given realname */
	private String realname = null;
	/** Given nickname */
	private String nickname = null;
	/** Given password */
	private String password = null;
	
	/** The Account object for this connect (This is null before authentication) */
	private Account myAccount;
	
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
	 * Action to take when socket is opened and ready.
	 */
	public void socketOpened() {
		sendLine("NOTICE AUTH :- Welcome to DFBnc ("+DFBnc.VERSION+")");
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
	 * Process a line of data.
	 *
	 * @param line Line to handle
	 */
	protected void processLine(final String line) {
		// Tokenise the line
		final String[] newLine = IRCParser.tokeniseLine(line);
		
		if (newLine.length < 2) {
			sendIRCLine(Consts.ERR_NEEDMOREPARAMS, newLine[0], "Not enough parameters");
			return;
		}
		
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
		if (line[0].equals("USER")) {
			// Username may be given in PASS so check that it hasn't before assigning
			if (username == null) { username = line[1]; }
			realname = line[line.length-1];
			if (nickname != null && password == null) {
				sendLine("NOTICE AUTH :- Please enter your password.");
				sendLine("NOTICE AUTH :- This can be done using either: ");
				sendLine("NOTICE AUTH :-     /QUOTE PASS [<username>:]<password");
				sendLine("NOTICE AUTH :-     /RAW PASS [<username>:]<password>");
			}
		} else if (line[0].equals("NICK")) {
			nickname = line[1];
			if (realname != null && password == null) {
				sendLine("NOTICE AUTH :- Please enter your password.");
				sendLine("NOTICE AUTH :- This can be done using either: ");
				sendLine("NOTICE AUTH :-     /QUOTE PASS [<username>:]<password");
				sendLine("NOTICE AUTH :-     /RAW PASS [<username>:]<password>");
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
				sendLine("NOTICE AUTH :- You are the first user of this bnc, and have been made admin");
				Config.saveAll(DFBnc.getConfigFileName());
			}
			if (Account.checkPassword(username, password)) {
				myAccount = Account.get(username);
				sendLine("NOTICE AUTH :- You are now logged in");
				if (myAccount.isAdmin()) {
					sendLine("NOTICE AUTH :- This is an Admin account");
				}
				close();
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
	private void processAuthenticated(final String[] line) { }
}

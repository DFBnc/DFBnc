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
package uk.org.dataforce.dfbnc.servers.irc;

import uk.org.dataforce.dfbnc.ConnectionHandler;
import uk.org.dataforce.dfbnc.Account;
import uk.org.dataforce.dfbnc.UserSocket;
import uk.org.dataforce.dfbnc.UnableToConnectException;
import com.dmdirc.parser.IRCParser;
import com.dmdirc.parser.MyInfo;
import com.dmdirc.parser.ServerInfo;
import com.dmdirc.parser.callbacks.interfaces.IDataIn;
import com.dmdirc.parser.callbacks.CallbackNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * This file represents an IRCConnectionHandler
 */
public class IRCConnectionHandler implements ConnectionHandler, IDataIn {
	/** Account that this IRCConnectionHandler is for */
	private final Account myAccount;
	/** IRCParser we are using. */
	private final IRCParser myParser;
	/** Thread used to store IRCParser */
	private final Thread controlThread;
	
	/**
	 * Shutdown this ConnectionHandler
	 *
	 * @param reason Reason for the Shutdown
	 */
	public void shutdown(final String reason) {
		myParser.disconnect(reason);
	}
	
	/**
	 * Called when data is recieved on the user socket.
	 *
	 * @param user The socket that the data arrived on
	 * @param data Data that was recieved
	 */
	public void dataRecieved(final UserSocket user, final String data, final String[] line) {
		myParser.sendLine(data);
	}
	
	/**
	 * Called on every incomming line BEFORE parsing.
	 * 
	 * @param tParser Reference to the parser object that made the callback.
	 * @param sData Incomming Line.
	 * @see com.dmdirc.parser.IRCParser#callDataIn
	 */
	public void onDataIn(IRCParser tParser, String sData) {
		for (UserSocket socket : myAccount.getUserSockets()) {
			socket.sendLine(sData);
		}
	}
	
	/**
	 * Create a new IRCConnectionHandler
	 *
	 * @param user User who requested the connection
	 * @param serverNumber Server number to use to connect, negative = random
	 */
	public IRCConnectionHandler(final UserSocket user, final int serverNumber) throws UnableToConnectException {
		this(user, user.getAccount(), serverNumber);
	}
	
	/**
	 * Create a new IRCConnectionHandler
	 *
	 * @param acc Account that requested the connection
	 * @param serverNum Server number to use to connect, negative = random
	 */
	public IRCConnectionHandler(final UserSocket user, final Account acc, final int serverNum) throws UnableToConnectException {
		myAccount = acc;
		MyInfo me = new MyInfo();
		me.setNickname(myAccount.getProperties().getProperty("irc.nickname", myAccount.getName()));
		me.setAltNickname(myAccount.getProperties().getProperty("irc.altnickname", "_"+myAccount.getName()));
		me.setRealname(myAccount.getProperties().getProperty("irc.realname", myAccount.getName()));
		me.setUsername(myAccount.getProperties().getProperty("irc.username", myAccount.getName()));
		
		List<String> serverList = new ArrayList<String>();
		serverList = user.getAccount().getProperties().getListProperty("irc.serverlist", serverList);
		if (serverList.size() == 0) { throw new UnableToConnectException("No servers found"); }
		
		int serverNumber = serverNum;
		if (serverNumber >= serverList.size() || serverNumber < 0) {
			serverNumber = (new Random()).nextInt(serverList.size());
		}
		String[] serverInfo = IRCServerType.parseServerString(serverList.get(serverNumber));
		ServerInfo server;
		try {
			server = new ServerInfo(serverInfo[0], Integer.parseInt(serverInfo[1]), serverInfo[2]);
		} catch (NumberFormatException nfe) {
			throw new UnableToConnectException("Invalid Port");
		}
		
		myParser = new IRCParser(me, server);
		try {
			myParser.getCallbackManager().addCallback("OnDataIn", this);
		} catch (CallbackNotFoundException cnfe) {
			throw new UnableToConnectException("Unable to register callbacks");
		}
		
		if (user != null) { user.sendBotMessage("Using server: "+serverInfo[3]); }
		
		controlThread = new Thread(myParser);
		controlThread.start();
	}
}
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
import uk.org.dataforce.dfbnc.Consts;
import uk.org.dataforce.dfbnc.UserSocket;
import uk.org.dataforce.dfbnc.UnableToConnectException;
import uk.org.dataforce.dfbnc.UserSocketWatcher;
import com.dmdirc.parser.IRCParser;
import com.dmdirc.parser.ClientInfo;
import com.dmdirc.parser.ChannelInfo;
import com.dmdirc.parser.ChannelClientInfo;
import com.dmdirc.parser.MyInfo;
import com.dmdirc.parser.ServerInfo;
import com.dmdirc.parser.callbacks.interfaces.IDataIn;
import com.dmdirc.parser.callbacks.interfaces.IServerReady;
import com.dmdirc.parser.callbacks.interfaces.INumeric;
import com.dmdirc.parser.callbacks.interfaces.IPost005;
import com.dmdirc.parser.callbacks.interfaces.IMOTDEnd;
import com.dmdirc.parser.callbacks.CallbackNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * This file represents an IRCConnectionHandler
 */
public class IRCConnectionHandler implements ConnectionHandler, UserSocketWatcher, IDataIn, IServerReady, IPost005, INumeric, IMOTDEnd {
	/** Account that this IRCConnectionHandler is for */
	private final Account myAccount;
	/** IRCParser we are using. */
	private final IRCParser myParser;
	/** Thread used to store IRCParser */
	private final Thread controlThread;
	/** Have we recieved a post005 callback? */
	private boolean hasPost005 = false;
	/** Have we recieved a MOTDEnd callback? */
	private boolean hasMOTDEnd = false;
	/** This stores the 002-005 lines which are sent to users who connect after we recieve them  */
	private List<String> connectionLines = new ArrayList<String>();
	
	/**
	 * Shutdown this ConnectionHandler
	 *
	 * @param reason Reason for the Shutdown
	 */
	@Override
	public void shutdown(final String reason) {
		myParser.disconnect(reason);
	}
	
	/**
	 * Called when data is recieved on the user socket.
	 *
	 * @param user The socket that the data arrived on
	 * @param data Data that was recieved
	 */
	@Override
	public void dataRecieved(final UserSocket user, final String data, final String[] line) {
		StringBuilder outData = new StringBuilder();
		if (line[0].equalsIgnoreCase("topic") || line[0].equalsIgnoreCase("names") || line[0].equalsIgnoreCase("mode")) {
			if (handleCommandProxy(user, line, outData)) {
				for (String channelName : line[1].split(",")) {
					ClientInfo client = myParser.getClientInfo(channelName);
					ChannelInfo channel = myParser.getChannelInfo(channelName);
					if (channel != null || line[0].equalsIgnoreCase("mode")) {
						if (line[0].equalsIgnoreCase("topic")) {
							sendTopic(user, channel);
						} else if (line[0].equalsIgnoreCase("topic")) {
							sendNames(user, channel);
						} else if (line[0].equalsIgnoreCase("mode")) {
							if (channel != null) {
								user.sendIRCLine(324, myParser.getMyNickname()+" "+channel, channel.getModeStr(), false);
								if (channel.getCreateTime() > 0) {
									user.sendIRCLine(329, myParser.getMyNickname()+" "+channel, ""+channel.getCreateTime(), false);
								}
							} else if (client == myParser.getMyself()) {
								user.sendIRCLine(221, myParser.getMyNickname(), client.getUserModeStr(), false);
							} else {
								if (outData.length() == 0) { outData.append(line[0].toUpperCase()); }
								outData.append(" "+channelName);
							}
						}
					} else {
						if (outData.length() == 0) { outData.append(line[0].toUpperCase()); }
						outData.append(" "+channelName);
					}
				}
			}
			if (outData.length() == 0) { return; }
		} else if (line[0].equalsIgnoreCase("quit")) {
			return;
		}
		
		if (outData.length() == 0) {
			myParser.sendLine(data);
		} else {
			System.out.println("Sending: "+outData.toString());
			myParser.sendLine(outData.toString());
		}
	}
	
	/**
	 * This function does the grunt work for dataRecieved.
	 * This function checks for -f in the first param, and if its there returns
	 * false and modifies outData.
	 * This function also returns false if line.length > 2
	 *
	 * @param user User who send the command
	 * @param line Input tokenised line
	 * @param outData This StringBuilder will be modified if needed. If result is
	 *                false, this StringBuilder will contain the line needed to be
	 *                send to the server. (If this is empty, nothing should be sent)
	 * @return true if we should handle this command, else false.
	 */
	public boolean handleCommandProxy(final UserSocket user, final String[] line, final StringBuilder outData) {
		// If line length is 2 or 3
		// ie (/topic #foo or /topic -f #foo or /topic #foo bar)
		if (line.length == 2 || line.length == 3) {
			// if (/topic -f)
			if (line[1].equalsIgnoreCase("-f")) {
				// if (/topic -f #foo)
				if (line.length == 3) {
					outData.append(line[0]+" "+line[2]);
					return false;
				} else {
					user.sendIRCLine(Consts.ERR_NEEDMOREPARAMS, line[0], "Not enough parameters-");
					return false;
				}
			// if /topic #foo
			} else if (line.length == 2) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Servername to use in sendIRCLine and Bot SNOTICEs as an alternative to
	 * Functions.getServerName() or null if no change.
	 *
	 * @return servername to use.
	 */
	@Override
	public String getServerName() {
		if (myParser.isReady()) {
			return myParser.getServerName();
		} else {
			return null;
		}
	}
	
	/**
	 * Called on every incomming line BEFORE parsing.
	 *
	 * @param tParser Reference to the parser object that made the callback.
	 * @param sData Incomming Line.
	 * @see com.dmdirc.parser.IRCParser#callDataIn
	 */
	@Override
	public void onDataIn(IRCParser tParser, String sData) {
		for (UserSocket socket : myAccount.getUserSockets()) {
			socket.sendLine(sData);
		}
	}
	
	/**
	 * Called after 001.
	 * 
	 * @param tParser Reference to the parser object that made the callback.
	 * @see com.dmdirc.parser.Process001#callServerReady
	 */
	@Override
	public void onServerReady(IRCParser tParser) {
		for (UserSocket socket : myAccount.getUserSockets()) {
			socket.setPost001(true);
		}
	}
	
	/**
	 * Called after 005.
	 * 
	 * @param tParser Reference to the parser object that made the callback.
	 * @see com.dmdirc.parser.Process001#callPost005
	 */
	@Override
	public void onPost005(IRCParser tParser) {
		hasPost005 = true;
		// We no longer need this callback, so lets remove it
		myParser.getCallbackManager().delCallback("OnNumeric", this);
	}
	
	/**
	 * Called when "End of MOTD" or "No MOTD".
	 *
	 * @param tParser Reference to the parser object that made the callback.
	 * @param noMOTD Set to true if this was a "No MOTD Found" message rather than an "End of MOTD"
	 * @param sData The contents of the line (incase of language changes or so)
	 * @see com.dmdirc.parser.ProcessMOTD#callMOTDEnd
	 */
	@Override
	public void onMOTDEnd(IRCParser tParser, boolean noMOTD, String sData) {
		hasMOTDEnd = true;
	}
	
	/**
	 * Called on every incomming line with a numerical type.
	 * 
	 * @param tParser Reference to the parser object that made the callback.
	 * @param numeric What numeric is this for
	 * @param token IRC Tokenised line
	 * @see com.dmdirc.parser.ProcessingManager#callNumeric
	 */
	@Override
	public void onNumeric(IRCParser tParser, int numeric, String[] token) {
		if (numeric > 1 && numeric < 6) {
			connectionLines.add(tParser.getLastLine());
		}
	}
	
	/**
	 * Called when a new UserSocket is opened on an account that this class is
	 * linked to.
	 *
	 * @param user UserSocket for user
	 */
	@Override
	public void userConnected(final UserSocket user) {
		// If the parser has processed a 001, we need to send our own
		if (myParser.isReady()) {
			user.sendIRCLine(1, myParser.getMyNickname(), "Welcome to the Internet Relay Network, "+myParser.getMyNickname());
			// Now send any of the 002-005 lines that we have
			for (String line : connectionLines) {
				user.sendLine(line);
			}
			// Now, if the parser has recieved an end of MOTD Line, we should send our own MOTD and User Host info
			if (hasMOTDEnd) {
				user.sendIRCLine(375, myParser.getMyNickname(), "- "+myParser.getServerName()+" Message of the Day -");
				user.sendIRCLine(372, myParser.getMyNickname(), "You are connected to an IRC Server, please type /MOTD to get the server's MOTD.");
				user.sendIRCLine(376, myParser.getMyNickname(), "End of /MOTD command.");
				
				// Now send 302 to let the client know its userhost
				// also send a 306 if the user is away so that the client can update itself
				ClientInfo me = myParser.getMyself();
				StringBuilder str302 = new StringBuilder(me.getNickname());
				if (me.isOper()) { str302.append("*"); }
				str302.append("=");
				if (me.getAwayState()) {
					user.sendIRCLine(306, myParser.getMyNickname(), "You have been marked as being away");
					str302.append("-");
				} else {
					str302.append("+");
				}
				str302.append(me.getIdent()+"@"+me.getHost());
				user.sendIRCLine(302, myParser.getMyNickname(), str302.toString());
				// Now send the usermode info
				user.sendIRCLine(221, myParser.getMyNickname(), me.getUserModeStr(), false);
				
				// Rejoin channels
				for (ChannelInfo channel : myParser.getChannels()) {
					user.sendLine(":%s JOIN %s", me, channel);
					user.sendIRCLine(324, myParser.getMyNickname()+" "+channel, channel.getModeStr(), false);
					if (channel.getCreateTime() > 0) {
						user.sendIRCLine(329, myParser.getMyNickname()+" "+channel, ""+channel.getCreateTime(), false);
					}
					
					sendTopic(user, channel);
					sendNames(user, channel);
				}
			}
		}
	}
	
	/**
	 * Send a topic reply for a channel to the given user
	 *
	 * @param user User to send reply to
	 * @param channel Channel to send reply for
	 */
	public void sendTopic(final UserSocket user, final ChannelInfo channel) {
		if (!channel.getTopic().isEmpty()) {
			user.sendIRCLine(332, myParser.getMyNickname()+" "+channel, channel.getTopic());
			user.sendIRCLine(333, myParser.getMyNickname()+" "+channel, channel.getTopicUser()+" "+channel.getTopicTime(), false);
		} else {
			user.sendIRCLine(331, myParser.getMyNickname()+" "+channel, "No topic is set.");
		}
	}
	
	/**
	 * Send a names reply for a channel to the given user
	 *
	 * @param user User to send reply to
	 * @param channel Channel to send reply for
	 */
	public void sendNames(final UserSocket user, final ChannelInfo channel) {
		final int maxLength = 500-(":"+getServerName()+" 353 "+myParser.getMyNickname()+" = "+channel+" :").length();
		StringBuilder names = new StringBuilder();
		for (ChannelClientInfo cci : channel.getChannelClients()) {
			if (cci.toString().length() > (maxLength-names.length())) {
				user.sendIRCLine(353, myParser.getMyNickname()+" = "+channel, names.toString().trim());
				names = new StringBuilder();
			}
			names.append(cci.toString()).append(" ");
		}
		if (names.length() > 0) {
			user.sendIRCLine(353, myParser.getMyNickname()+" = "+channel, names.toString().trim());
		}
		user.sendIRCLine(366, myParser.getMyNickname()+" "+channel, "End of /NAMES list. (Cached)");
	}
	
	/**
	 * Called when a UserSocket is closed on an account that this class is
	 * linked to.
	 *
	 * @param user UserSocket for user
	 */
	@Override
	public void userDisconnected(final UserSocket user) {
	}
	
	/**
	 * Create a new IRCConnectionHandler
	 *
	 * @param user User who requested the connection
	 * @param serverNumber Server number to use to connect, negative = random
	 * @throws UnableToConnectException If there is a problem connecting to the server
	 */
	public IRCConnectionHandler(final UserSocket user, final int serverNumber) throws UnableToConnectException {
		this(user, user.getAccount(), serverNumber);
	}
	
	/**
	 * Create a new IRCConnectionHandler
	 *
	 * @param user User who requested the connection
	 * @param acc Account that requested the connection
	 * @param serverNum Server number to use to connect, negative = random
	 * @throws UnableToConnectException If there is a problem connecting to the server
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
			boolean isSSL = false;
			if (serverInfo[0].charAt(0) == '@') {
				serverInfo[0] = serverInfo[0].substring(1);
				isSSL = true;
			}
			server = new ServerInfo(serverInfo[0], Integer.parseInt(serverInfo[1]), serverInfo[2]);
			server.setSSL(isSSL);
		} catch (NumberFormatException nfe) {
			throw new UnableToConnectException("Invalid Port");
		}
		
		myParser = new IRCParser(me, server);
		try {
			myParser.getCallbackManager().addCallback("OnDataIn", this);
			myParser.getCallbackManager().addCallback("OnServerReady", this);
			myParser.getCallbackManager().addCallback("OnPost005", this);
			myParser.getCallbackManager().addCallback("OnNumeric", this);
			myParser.getCallbackManager().addCallback("OnMOTDEnd", this);
		} catch (CallbackNotFoundException cnfe) {
			throw new UnableToConnectException("Unable to register callbacks");
		}
		
		if (user != null) { user.sendBotMessage("Using server: "+serverInfo[3]); }
		
		controlThread = new Thread(myParser);
		controlThread.start();
	}
}
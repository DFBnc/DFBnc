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
import com.dmdirc.parser.ChannelClientInfo;
import com.dmdirc.parser.ChannelInfo;
import com.dmdirc.parser.ChannelListModeItem;
import com.dmdirc.parser.MyInfo;
import com.dmdirc.parser.ServerInfo;
import com.dmdirc.parser.callbacks.interfaces.IDataIn;
import com.dmdirc.parser.callbacks.interfaces.IServerReady;
import com.dmdirc.parser.callbacks.interfaces.INickChanged;
import com.dmdirc.parser.callbacks.interfaces.INumeric;
import com.dmdirc.parser.callbacks.interfaces.IPost005;
import com.dmdirc.parser.callbacks.interfaces.IMOTDEnd;
import com.dmdirc.parser.callbacks.interfaces.IChannelSelfJoin;
import com.dmdirc.parser.callbacks.CallbackNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This file represents an IRCConnectionHandler.
 *
 * It handles parser callbacks, and proxies data between users and the server.
 * It also handles the performs.
 */
public class IRCConnectionHandler implements ConnectionHandler, UserSocketWatcher, IDataIn, INickChanged, IServerReady, IPost005, INumeric, IMOTDEnd, IChannelSelfJoin {
	/**
	 * This stores a line that is being requeued.
	 */
	private class RequeueLine {
		/** What user reqeusted this line? */
		final UserSocket user;
		/** The line */
		final String line;
		/** How many times has this line been requeued before? */
		final int times;
		
		/**
		 * Create a new RequeueLine
		 *
		 * @param user What user reqeusted this line?
		 * @param line The line
		 * @param times How many times has this line been requeued before?
		 *
		 */
		public RequeueLine(final UserSocket user, final String line, final int times) {
			this.user = user;
			this.line = line;
			this.times = times;
		}
		
		/**
		 * Resend this line through the processor.
		 *
		 * @param connectionHandler the IRCConnectionHandler that this line should be reprocessed in.
		 */
		public void reprocess(final IRCConnectionHandler connectionHandler) {
			if (user.isOpen()) {
				connectionHandler.processDataRecieved(user, line, IRCParser.tokeniseLine(line), times+1);
			}
		}
	}
	
	/**
	 * This takes items from the requeue list, and requeues them.
	 */
	private class RequeueTimerTask extends TimerTask {
		/** The IRCConnectionHandler that owns this task */
		final IRCConnectionHandler connectionHandler;
		
		/** Create a new RequeueTimerTask */
		public RequeueTimerTask (final IRCConnectionHandler connectionHandler) {
			this.connectionHandler = connectionHandler;
		}
		
		/** Actually do stuff */
		public void run() {
			List<RequeueLine> list = connectionHandler.getRequeueList();
			for (RequeueLine line : list) {
				line.reprocess(connectionHandler);
			}
		}
	}
	
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
	/** Have we hacked in our own 005? (Shows support for LISTMODE) */
	private boolean hacked005 = false;
	/** This stores the 002-005 lines which are sent to users who connect after we recieve them  */
	private List<String> connectionLines = new ArrayList<String>();
	/** This stores tokens not related to a channel that we want to temporarily allow to come via onDataIn */
	private List<String> allowTokens = new ArrayList<String>();
	/** This stores lines that need to be processed at a later date. */
	private volatile List<RequeueLine> requeueList = new ArrayList<RequeueLine>();
	/** This timer handles reprocessing of items in the requeueList */
	private Timer requeueTimer = new Timer("requeueTimer");
	
	/**
	 * Shutdown this ConnectionHandler
	 *
	 * @param reason Reason for the Shutdown
	 */
	@Override
	public void shutdown(final String reason) {
		myParser.quit(reason);
	}
	
	/**
	 * Get the users host on this connection
	 *
	 * @return The users host on this connect
	 */
	public String getMyHost() {
		if (myParser != null && myParser.getMyself() != null) {
			return myParser.getMyself().toString();
		} else if (myParser != null) {
			return String.format("%s!@", myParser.getMyNickname());
		} else {
			return null;
		}
	}
	
	/**
	 * Called when data is recieved on the user socket.
	 * This intercepts topic/mode/names requests and handles them itself where possible
	 * unless the -f parameter is passed (ie /mode -f #channel)
	 * This is horrible code really, but it works.
	 *
	 * @param user The socket that the data arrived on
	 * @param data Data that was recieved
	 * @param line IRC Tokenised version of the data
	 */
	@Override
	public void dataRecieved(final UserSocket user, final String data, final String[] line) {
		processDataRecieved(user, data, line, 0);
	}
	
	/**
	 * This is called to process data that is recieved on the user socket.
	 * This intercepts topic/mode/names requests and handles them itself where possible
	 * unless the -f parameter is passed (ie /mode -f #channel)
	 * This is horrible code really, but it works.
	 *
	 * @param user The socket that the data arrived on
	 * @param data Data that was recieved
	 * @param line IRC Tokenised version of the data
	 * @param times Number of times this line has been sent through the processor (used by requeue)
	 */
	public void processDataRecieved(final UserSocket user, final String data, final String[] line, final int times) {
		StringBuilder outData = new StringBuilder();
		boolean resetOutData = false;
		if (line[0].equalsIgnoreCase("topic") || line[0].equalsIgnoreCase("names") || line[0].equalsIgnoreCase("mode") || line[0].equalsIgnoreCase("listmode")) {
			if (handleCommandProxy(user, line, outData)) {
				for (String channelName : line[1].split(",")) {
					if (resetOutData) {
						if (outData.length() > 0) {
//							System.out.println("Sending old outData: "+outData.toString());
							myParser.sendLine(outData.toString());
							outData = new StringBuilder();
						}
						resetOutData = false;
					}
					ClientInfo client = myParser.getClientInfo(channelName);
					ChannelInfo channel = myParser.getChannelInfo(channelName);
					if (channel != null || line[0].equalsIgnoreCase("mode")) {
						if (line[0].equalsIgnoreCase("topic")) {
							sendTopic(user, channel);
						} else if (line[0].equalsIgnoreCase("names")) {
							sendNames(user, channel);
						} else if (line[0].equalsIgnoreCase("mode") || line[0].equalsIgnoreCase("listmode")) {
							if (channel != null) {
								boolean isListmode = line[0].equalsIgnoreCase("listmode");
								int itemNumber = 0;
								String listName = "";
								if (line.length == 3) {
									// If we can't actually answer this, requeue it to process later.
									// This makes the assumption that the callback will actually be fired,
									// which it may not be. Thus we only requeue the line if it hasn't
									// been through here more than 6 times. (This allows 25-30
									// seconds for a reply to our onJoin request for list modes)
									if (!channel.hasGotListModes() && times < 6) {
										synchronized (requeueList) {
											requeueList.add(new RequeueLine(user, String.format("%s %s %s", line[0], channelName, line[2]), times));
										}
										continue;
									}
									// Make sure we don't send the same thing twice. A list is probably overkill for this, but meh
									List<Character> alreadySent = new ArrayList<Character>();
									for (int i = 0; i < line[2].length(); ++i) {
										char modechar = line[2].charAt(i);
										if (alreadySent.contains(modechar)) { continue; }
										else { alreadySent.add(modechar); }
										final List<ChannelListModeItem> modeList = channel.getListModeParam(modechar);
										if (modeList != null) {
											// This covers most list items, if its not listed here it
											// gets forwarded to the server.
											if (modechar == 'b') { itemNumber = 367; listName = "Channel Ban List"; }
											else if (modechar == 'd') { itemNumber = 367; listName = "Channel Ban List"; }
											else if (modechar == 'q') { itemNumber = 367; listName = "Channel Ban List"; }
											else if (modechar == 'e') { itemNumber = 348; listName = "Channel Exception List"; }
											else if (modechar == 'I') { itemNumber = 346; listName = "Channel Invite List"; }
											else if (modechar == 'R') { itemNumber = 344; listName = "Channel Reop List"; }
											else {
												if (outData.length() == 0) { outData.append(line[0].toUpperCase()+" "+channelName+" "); }
												outData.append(line[2].charAt(i));
												resetOutData = true;
												allowLine(channel, Integer.toString(itemNumber));
												allowLine(channel, Integer.toString(itemNumber+1));
												continue;
											}
											String prefix = "";
											String thisIRCD = myParser.getIRCD(true).toLowerCase();
											if ((thisIRCD.equals("hyperion") || thisIRCD.equals("dancer")) && modechar == 'q') { prefix = "%"; }
											if (isListmode) {
												prefix = modechar+" "+prefix;
												itemNumber = 997;
												listName = "Channel List Modes";
											}
											for (ChannelListModeItem item : modeList) {
												user.sendIRCLine(itemNumber, myParser.getMyNickname()+" "+channel, prefix+item.getItem()+" "+item.getOwner()+" "+item.getTime(), false);
											}
											if (!isListmode && (modechar == 'b' || modechar == 'q')) {
												// If we are emulating a hyperian ircd, we need to send these together, unless we are using listmode.
												if (thisIRCD.equals("hyperion") || thisIRCD.equals("dancer")) {
													List<ChannelListModeItem> newmodeList;
													if (modechar == 'b') { newmodeList = channel.getListModeParam('q'); alreadySent.add('q'); }
													else { newmodeList = channel.getListModeParam('b'); alreadySent.add('b'); }
													
													// This actually applies to the listmode being q, but the requested mode was b, so we check that
													if (modechar == 'b') { prefix = "%"; } else { prefix = ""; }
													for (ChannelListModeItem item : newmodeList) {
														user.sendIRCLine(itemNumber, myParser.getMyNickname()+" "+channel, prefix+item.getItem()+" "+item.getOwner()+" "+item.getTime(), false);
													}
												}
											}
											if (!isListmode) {
	 											user.sendIRCLine(itemNumber+1, myParser.getMyNickname()+" "+channel, "End of "+listName+" (Cached)");
	 										}
										} else {
											if (outData.length() == 0) { outData.append(line[0].toUpperCase()+" "+channelName+" "); }
											outData.append(line[2].charAt(i));
											resetOutData = true;
										}
									}
									if (isListmode) {
										user.sendIRCLine(itemNumber+1, myParser.getMyNickname()+" "+channel, "End of "+listName+" (Cached)");
									}
								} else {
									// This will only actually be 0 if we havn't recieved the initial
									// on-Join reply from the server.
									// In this case the actual 324 and the 329 from the server will get
									// through to the client
									if (channel.getCreateTime() > 0) {
										user.sendIRCLine(324, myParser.getMyNickname()+" "+channel, channel.getModeStr(), false);
										user.sendIRCLine(329, myParser.getMyNickname()+" "+channel, ""+channel.getCreateTime(), false);
									} else {
										allowLine(channel, "324");
										allowLine(channel, "329");
									}
								}
							} else if (client == myParser.getMyself()) {
								user.sendIRCLine(221, myParser.getMyNickname(), client.getUserModeStr(), false);
							} else {
								if (outData.length() == 0) { outData.append(line[0].toUpperCase()+" "); }
								else { outData.append(","); }
								outData.append(channelName);
							}
						}
					} else {
						if (outData.length() == 0) { outData.append(line[0].toUpperCase()+" "); }
						else { outData.append(","); }
						outData.append(channelName);
					}
				}
			}
			if (outData.length() == 0) { return; }
		} else if (line[0].equalsIgnoreCase("quit")) {
			return;
		}
		
		if (outData.length() == 0) {
//			System.out.println("Sending: "+data);
			myParser.sendLine(data);
		} else {
//			System.out.println("Sending: "+outData.toString());
			myParser.sendLine(outData.toString());
		}
	}
	
	/**
	 * Get the requeueList.
	 * This is used by the requeueTimer, it returns a clone of the requeueList,
	 * and then empties the requeueList.
	 *
	 * @return Clone of the requeueList
	 */
	protected List<RequeueLine> getRequeueList() {
		List<RequeueLine> result;
		synchronized (requeueList) {
			result = new ArrayList<RequeueLine>(requeueList);
			requeueList.clear();
		}
		return result;
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
		// if (/topic -f)
		if (line.length == 0) {
			user.sendIRCLine(Consts.ERR_NEEDMOREPARAMS, line[0], "Not enough parameters");
			return false;
		}
		
//		System.out.println("Length: "+line.length);
//		for (String bit : line) {
//			System.out.println("Line: "+bit);
//		}
		
		if (line[1].equalsIgnoreCase("-f")) {
			// if (/topic -f #foo)
			if (line.length == 3 || line.length == 4) {
				outData.append(line[0]);
				outData.append(" ");
				outData.append(line[2]);
				if (line.length == 4) {
					outData.append(line[0].equalsIgnoreCase("topic") ? " :" : " ");
					outData.append(line[3]);
				}
				ChannelInfo channel = myParser.getChannelInfo(line[2]);
				if (line[0].equalsIgnoreCase("topic")) {
					allowLine(channel, "331");
					allowLine(channel, "332");
					allowLine(channel, "333");
				} else if(line[0].equalsIgnoreCase("names")) {
					allowLine(channel, "353");
					allowLine(channel, "366");
				} else if(line[0].equalsIgnoreCase("mode")) {
					if (channel != null) {
						if (line.length == 4) {
							for (int i = 0; i < line[3].length(); ++i) {
								char modechar = line[3].charAt(i);
								List<ChannelListModeItem> modeList = channel.getListModeParam(modechar);
								if (modeList != null) {
									if (modechar == 'b' || modechar == 'd' || modechar == 'q') {
										allowLine(channel, "367");
										allowLine(channel, "368");
									} else if (modechar == 'e') {
										allowLine(channel, "348");
										allowLine(channel, "349");
									} else if (modechar == 'I') {
										allowLine(channel, "346");
										allowLine(channel, "347");
									} else if (modechar == 'R') {
										allowLine(channel, "344");
										allowLine(channel, "345");
									}
								}
							}
						} else {
							allowLine(channel, "324");
							allowLine(channel, "329");
						}
					} else {
						allowLine(channel, "221");
					}
				} else if(line[0].equalsIgnoreCase("listmode")) {
					if (myParser.get005().containsKey("LISTMODE")) {
						allowLine(channel, myParser.get005().get("LISTMODE"));
						allowLine(channel, myParser.get005().get("LISTMODEEND"));
					}
				}
				return false;
			} else {
				if (line.length < 3) {
					user.sendIRCLine(Consts.ERR_NEEDMOREPARAMS, line[0], "Not enough parameters");
				} else {
					// Send line directly to server (without the -f param)
					outData.append(line[0]);
					for (int i = 2; i < line.length; i++) {
						outData.append(" ");
						outData.append(line[i]);
					}
				}
				return false;
			}
		// if /topic #foo
		} else if (line.length == 2) {
			return true;
		// ie /mode #channel b
		} else if (line.length == 3) {
			if (line[0].equalsIgnoreCase("mode") || line[0].equalsIgnoreCase("listmode")) {
//				System.out.println("Handle!");
				return true;
			} else if (line[0].equalsIgnoreCase("topic")) {
				outData.append(line[0]);
				outData.append(" ");
				outData.append(line[1]);
				outData.append(" :");
				outData.append(line[2]);
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
	 * Called when we or another user change nickname.
	 * This is called after the nickname change has been done internally
	 * 
	 * @param tParser Reference to the parser object that made the callback.
	 * @param cClient Client changing nickname
	 * @param sOldNick Nickname before change
	 * @see com.dmdirc.parser.ProcessNick#callNickChanged
	 */
	public void onNickChanged(IRCParser tParser, ClientInfo cClient, String sOldNick) {
		if (cClient == tParser.getMyself()) {
			for (UserSocket socket : myAccount.getUserSockets()) {
				socket.setNickname(tParser.getMyNickname());
			}
		}
	}
	
	/**
	 * Set a line type that we want to forward to the user.
	 * By default certain messages from the server are not forwarded to the user,
	 * this allows the command proxy to specify a token we want to allow for a 1
	 * time usage.
	 *
	 * @param channel Channel to allow line for (or null for the global list)
	 * @param token Token to allow
	 */
	@SuppressWarnings("unchecked")
	private void allowLine(final ChannelInfo channel, final String token) {
		List<String> tokens;
		if (channel != null) {
			tokens = (List<String>)channel.getMap().get("AllowedTokens");
			if (tokens == null) {
				tokens = new ArrayList<String>();
			}
		} else {
			tokens = allowTokens;
		}
		if (!tokens.contains(token)) {
			tokens.add(token);
		}
		if (channel != null) {
			channel.getMap().put("AllowedTokens", tokens);
		}
	}
	
	/**
	 * Set a line type that we want no longer want to forward to the user.
	 * By default certain messages from the server are not forwarded to the user,
	 * this allows onDataIn to disallow a line again.
	 *
	 * @param channel Channel to disallow line for (or null for the global list)
	 * @param token Token to disallow
	 */
	@SuppressWarnings("unchecked")
	private void disallowLine(final ChannelInfo channel, final String token) {
		List<String> tokens;
		if (channel != null) {
			tokens = (List<String>)channel.getMap().get("AllowedTokens");
			if (tokens == null) {
				tokens = new ArrayList<String>();
			}
		} else {
			tokens = allowTokens;
		}
		if (tokens.contains(token)) {
			tokens.remove(token);
		}
		if (channel != null) {
			channel.getMap().put("AllowedTokens", tokens);
		}
	}
	
	/**
	 * Check if a line is temporarily allowed.
	 *
	 * @param channel Channel to check line allowance for (or null for the global list)
	 * @param token token to check
	 */
	@SuppressWarnings("unchecked")
	private boolean checkAllowLine(final ChannelInfo channel, final String token) {
		List<String> tokens;
		if (channel != null) {
			tokens = (List<String>)channel.getMap().get("AllowedTokens");
			if (tokens == null) {
				tokens = new ArrayList<String>();
			}
		} else {
			tokens = allowTokens;
		}
		
		return tokens.contains(token);
	}
	
	/**
	 * Called When we join a channel.
	 * We are NOT added as a channelclient until after the names reply
	 *
	 * @param tParser Reference to the parser object that made the callback.
	 * @param cChannel Channel Object
	 * @see com.dmdirc.parser.ProcessJoin#callChannelSelfJoin
	 */
	public void onChannelSelfJoin(IRCParser tParser, ChannelInfo cChannel) {
		// Allow Names Through
		allowLine(cChannel, "353");
		allowLine(cChannel, "366");
		// Allow Topic Through
		allowLine(cChannel, "331");
		allowLine(cChannel, "332");
		allowLine(cChannel, "333");
	}
	
	/**
	 * Called on every incomming line BEFORE parsing.
	 * We use this to choose what lines should or shouldn't be forwarded to the user.
	 * We block everything that we proxy unless it was forwaded to the server.
	 *
	 * @param tParser Reference to the parser object that made the callback.
	 * @param sData Incomming Line.
	 * @see com.dmdirc.parser.IRCParser#callDataIn
	 */
	@Override
	public void onDataIn(IRCParser tParser, String sData) {
		boolean forwardLine = true;
		final String[] bits = IRCParser.tokeniseLine(sData);
		try {
			final int numeric = Integer.parseInt(bits[1]);
			final boolean supportLISTMODE = myParser.get005().containsKey("LISTMODE");
			if (supportLISTMODE) {
				if (bits[1].equals(myParser.get005().get("LISTMODE"))) {
					return;
				} else if (bits[1].equals(myParser.get005().get("LISTMODEEND"))) {
					return;
				}
			}
			// Don't forward pongs from the server
			if (bits[1].equals("PONG") || bits[0].equals("PONG")) { return; }
			final ChannelInfo channel = (bits.length > 3) ? myParser.getChannelInfo(bits[3]) : null;
			switch (numeric) {
				case 324: // Channel Modes
				case 367: // Ban List
				case 348: // Exception List
				case 346: // Invite List
				case 344: // Reop List
				case 332: // Topic
					forwardLine = checkAllowLine(channel, bits[1]);
					break;
				
				case 353: // Names
					forwardLine = checkAllowLine(myParser.getChannelInfo(bits[4]), bits[1]);
					break;
					
				case 368: // Ban List End
				case 349: // Exception List End
				case 347: // Invite List End
				case 345: // Reop List End
					forwardLine = checkAllowLine(channel, bits[1]);
					if (forwardLine) {
						disallowLine(channel, bits[1]);
						disallowLine(channel, Integer.toString(numeric-1));
					}
					break;
				
				case 329: // Channel Create Time
					forwardLine = checkAllowLine(channel, bits[1]);
					if (forwardLine) {
						disallowLine(channel, bits[1]);
						disallowLine(channel, "324");
					}
					break;

				case 221: // User Modes
					forwardLine = checkAllowLine(channel, bits[1]);
					if (forwardLine) {
						disallowLine(channel, bits[1]);
					}
					break;

				case 331: // Topic Time/User
				case 333: // No Topic
					forwardLine = checkAllowLine(channel, bits[1]);
					if (forwardLine) {
						disallowLine(channel, "331");
						disallowLine(channel, "332");
						disallowLine(channel, "333");
					}
					break;

				case 366: // Names End
					forwardLine = checkAllowLine(channel, bits[1]);
					if (forwardLine) {
						disallowLine(channel, bits[1]);
						disallowLine(channel, "353");
					}
					break;
			}
		} catch (NumberFormatException nfe) { }
		
		if (forwardLine) {
			for (UserSocket socket : myAccount.getUserSockets()) {
				socket.sendLine(sData);
			}
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
			socket.setNickname(tParser.getMyNickname());
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
		List<String> myList = new ArrayList<String>();
		myList = myAccount.getProperties().getListProperty("irc.perform.connect", myList);
		for (String line : myList) {
			myParser.sendLine(filterPerformLine(line));
		}
		if (myAccount.getUserSockets().size()  == 0) {
			myList = new ArrayList<String>();
			myList = myAccount.getProperties().getListProperty("irc.perform.lastdetach", myList);
			for (String line : myList) {
				myParser.sendLine(filterPerformLine(line));
			}
		}
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
			if (numeric == 5 && !hacked005) {
				// Add our own 005.
				// * Show support for advanced LISTMODE (http://shane.dmdirc.com/listmodes.php)
				// * Show that this is a BNC Connection
				final String my005 = ":"+getServerName()+" 005 "+myParser.getMyNickname()+" LISTMODE=997 BNC=DFBNC :are supported by this server";
				for (UserSocket socket : myAccount.getUserSockets()) {
					socket.sendLine(my005);
				}
				connectionLines.add(my005);
				hacked005 = true;
			}
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
					
					sendTopic(user, channel);
					sendNames(user, channel);
				}
			}
			if (myAccount.getUserSockets().size() == 1) {
				List<String> myList = new ArrayList<String>();
				myList = myAccount.getProperties().getListProperty("irc.perform.firstattach", myList);
				for (String line : myList) {
					myParser.sendLine(filterPerformLine(line));
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
		if (myParser.isReady()) {
			if (myAccount.getUserSockets().size() == 0) {
				List<String> myList = new ArrayList<String>();
				myList = myAccount.getProperties().getListProperty("irc.perform.lastdetach", myList);
				for (String line : myList) {
					myParser.sendLine(filterPerformLine(line));
				}
			}
		}
	}
	
	/**
	 * Filter a perform line and return the line after substitutions have occured
	 *
	 * @return Processed line
	 */
	public String filterPerformLine(final String input) {
		String result = input;
		result = result.replaceAll("$me", myParser.getMyNickname());
		return result;
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
		// Reprocess queued items every 5 seconds.
		requeueTimer.scheduleAtFixedRate(new RequeueTimerTask(this), 0, 5000);
		// Allow the initial usermode line through to the user
		allowLine(null, "221");
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
			final int portNum;
			if (serverInfo[1].charAt(0) == '+') {
				portNum = Integer.parseInt(serverInfo[1].substring(1));
				isSSL = true;
			} else {
				portNum = Integer.parseInt(serverInfo[1]);
			}
			server = new ServerInfo(serverInfo[0], portNum, serverInfo[2]);
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
			myParser.getCallbackManager().addCallback("OnChannelSelfJoin", this);
			myParser.getCallbackManager().addCallback("OnNickChanged", this);
		} catch (CallbackNotFoundException cnfe) {
			throw new UnableToConnectException("Unable to register callbacks");
		}
		
		if (user != null) { user.sendBotMessage("Using server: "+serverInfo[3]); }
		
		final String bindIP = myAccount.getProperties().getProperty("irc.bindip", "");
		if (!bindIP.isEmpty()) {
			myParser.setBindIP(bindIP);
			user.sendBotMessage("Trying to bind to: "+bindIP);
		}
		
		controlThread = new Thread(myParser);
		controlThread.start();
	}
}

/*
 * Copyright (c) 2006-2013 Shane Mc Cormack
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
 */

package uk.org.dataforce.dfbnc.servers.irc;

import com.dmdirc.parser.common.AwayState;
import com.dmdirc.parser.common.CallbackNotFoundException;
import com.dmdirc.parser.common.ChannelListModeItem;
import com.dmdirc.parser.common.MyInfo;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.irc.CapabilityState;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.irc.IRCChannelInfo;
import com.dmdirc.parser.irc.IRCClientInfo;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.callbacks.CallbackInterface;
import com.dmdirc.parser.interfaces.callbacks.DataInListener;
import com.dmdirc.parser.interfaces.callbacks.DataOutListener;
import com.dmdirc.parser.interfaces.callbacks.ServerReadyListener;
import com.dmdirc.parser.interfaces.callbacks.NickChangeListener;
import com.dmdirc.parser.interfaces.callbacks.NumericListener;
import com.dmdirc.parser.interfaces.callbacks.MotdEndListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelJoinListener;
import com.dmdirc.parser.interfaces.callbacks.ChannelSelfJoinListener;
import com.dmdirc.parser.interfaces.callbacks.ConnectErrorListener;
import com.dmdirc.parser.interfaces.callbacks.ErrorInfoListener;
import com.dmdirc.parser.interfaces.callbacks.SocketCloseListener;

import com.dmdirc.parser.irc.IRCChannelClientInfo;
import com.dmdirc.parser.irc.ServerType;
import com.dmdirc.parser.irc.ServerTypeGroup;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import uk.org.dataforce.dfbnc.BackbufferMessage;
import uk.org.dataforce.dfbnc.ConnectionHandler;
import uk.org.dataforce.dfbnc.Account;
import uk.org.dataforce.dfbnc.Consts;
import uk.org.dataforce.dfbnc.RollingList;
import uk.org.dataforce.dfbnc.config.ConfigChangedListener;
import uk.org.dataforce.dfbnc.sockets.UserSocket;
import uk.org.dataforce.dfbnc.sockets.UnableToConnectException;
import uk.org.dataforce.dfbnc.sockets.UserSocketWatcher;
import uk.org.dataforce.libs.logger.Logger;
import uk.org.dataforce.libs.util.Util;


/**
 * This file represents an IRCConnectionHandler.
 *
 * It handles parser callbacks, and proxies data between users and the server.
 * It also handles the performs.
 */
public class IRCConnectionHandler implements ConnectionHandler,
        UserSocketWatcher, DataInListener, DataOutListener, NickChangeListener,
        ServerReadyListener, NumericListener, MotdEndListener,
        SocketCloseListener, ChannelSelfJoinListener, ConnectErrorListener,
        ErrorInfoListener, ConfigChangedListener, ChannelJoinListener {

    /** Account that this IRCConnectionHandler is for. */
    private final Account myAccount;
    /** Server we were supposed to connect to. */
    private final int myServerNum;
    /** IRCParser we are using. */
    private final Parser myParser;
    /** Have we received a ServerReady callback? */
    private boolean parserReady = false;
    /** Have we received a MOTDEnd callback? */
    private boolean hasMOTDEnd = false;
    /** Have we hacked in our own 005? (Shows support for LISTMODE) */
    private boolean hacked005 = false;
    /** This stores the 002-005 lines which are sent to users who connect after we receive them  */
    private List<String> connectionLines = new ArrayList<String>();
    /** This stores tokens not related to a channel that we want to temporarily allow to come via onDataIn */
    private List<String> allowTokens = new ArrayList<String>();
    /** This stores lines that need to be processed at a later date. */
    private final List<RequeueLine> requeueList = new ArrayList<RequeueLine>();
    /** This timer handles re-processing of items in the requeueList */
    private Timer requeueTimer = new Timer("requeueTimer");

    /**
     * Create a new IRCConnectionHandler
     *
     * @param acc Account that requested the connection
     * @param serverNum Server number to use to connect, negative = random
     * @throws UnableToConnectException If there is a problem connecting to the server
     */
    public IRCConnectionHandler(final Account acc, final int serverNum) throws UnableToConnectException {
        myAccount = acc;
        myServerNum = serverNum;
        MyInfo me = new MyInfo();
        me.setNickname(myAccount.getConfig().getOption("irc", "nickname", myAccount.getName()));
        me.setAltNickname(myAccount.getConfig().getOption("irc", "altnickname", "_" + me.getNickname()));
        me.setRealname(myAccount.getConfig().getOption("irc", "realname", myAccount.getName()));
        me.setUsername(myAccount.getConfig().getOption("irc", "username", myAccount.getName()));

        List<String> serverList = new ArrayList<String>();
        serverList = acc.getConfig().getListOption("irc", "serverlist", serverList);

        if (serverList.isEmpty()) {
            throw new UnableToConnectException("No servers found");
        }

        int serverNumber = serverNum;
        if (serverNumber >= serverList.size() || serverNumber < 0) {
            serverNumber = (new Random()).nextInt(serverList.size());
        }

        String[] serverInfo = IRCServerType.parseServerString(serverList.get(serverNumber));
        URI server;
        try {
            boolean isSSL = false;
            final int portNum;
            if (serverInfo[1].charAt(0) == '+') {
                portNum = Integer.parseInt(serverInfo[1].substring(1));
                isSSL = true;
            } else {
                portNum = Integer.parseInt(serverInfo[1]);
            }
            server = new URI(isSSL ? "ircs" : "irc", serverInfo[2], serverInfo[0], portNum, "", "", "");
        } catch (NumberFormatException nfe) {
            throw new UnableToConnectException("Invalid Port");
        } catch (URISyntaxException use) {
            throw new UnableToConnectException("Unable to create URI: "+use);
        }

        myParser = new IRCParser(me, server);

        try {
            setupCallbacks();
        } catch (CallbackNotFoundException cnfe) {
            throw new UnableToConnectException("Unable to register callbacks");
        }

        acc.sendBotMessage("Using server: " + serverInfo[3]);

        final String bindIP = myAccount.getConfig().getOption("irc", "bindip", "");
        if (!bindIP.isEmpty()) {
            myParser.setBindIP(bindIP);
            acc.sendBotMessage("Trying to bind to: " + bindIP);
        }

        // Reprocess queued items every 5 seconds.
        requeueTimer.scheduleAtFixedRate(new RequeueTimerTask(this), 0, 5000);
        // Allow the initial usermode line through to the user
        allowLine(null, "221");

        myParser.connect();
        ((IRCParser)myParser).getControlThread().setName("IRC Parser - " + myAccount.getName() + " - <server>");
    }

    /**
     * Set up the callbacks with the parser.
     *
     * @throws CallbackNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void setupCallbacks() throws CallbackNotFoundException {
        for (Class c : IRCConnectionHandler.class.getInterfaces()) {
            if (CallbackInterface.class.isAssignableFrom(c)) {
                myParser.getCallbackManager().addCallback(c, this);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionHandler newInstance() throws UnableToConnectException {
        return new IRCConnectionHandler(myAccount, myServerNum);
    }

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
     * Get the users host on this connection
     *
     * @return The users host on this connect
     */
    @Override
    public String getMyHost() {
        if (myParser != null && myParser.getLocalClient() != null) {
            return myParser.getLocalClient().toString();
        } else if (myParser != null) {
            return String.format("%s!@", myParser.getLocalClient().getNickname());
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

        if (line.length > 1 && myParser.isValidChannelName(line[1]) && !user.allowedChannel(line[1])) {
            user.sendIRCLine(403, myParser.getLocalClient().getNickname() + " " + line[1], "Channel is not whitelisted for this client", true);
            return;
        }

        if (line[0].equalsIgnoreCase("topic") || line[0].equalsIgnoreCase("names") || line[0].equalsIgnoreCase("mode") || line[0].equalsIgnoreCase("listmode")) {
            if (handleCommandProxy(user, line, outData)) {
                for (String channelName : line[1].split(",")) {
                    if (resetOutData) {
                        if (outData.length() > 0) {
                            myParser.sendRawMessage(outData.toString());
                            outData = new StringBuilder();
                        }
                        resetOutData = false;
                    }
                    ClientInfo client = myParser.getClient(channelName);
                    ChannelInfo channel = myParser.getChannel(channelName);
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
                                    if (!((IRCChannelInfo) channel).hasGotListModes() && times < 6) {
                                        synchronized (requeueList) {
                                            requeueList.add(new RequeueLine(user, String.format("%s %s %s", line[0], channelName, line[2]), times));
                                        }
                                        continue;
                                    }
                                    // Make sure we don't send the same thing twice. A list is probably overkill for this, but meh
                                    final List<Character> alreadySent = new ArrayList<Character>();
                                    final String modeCharList = (isListmode && line[2].equals("*")) ? myParser.getListChannelModes() : line[2];

                                    for (int i = 0; i < modeCharList.length(); ++i) {
                                        char modechar = modeCharList.charAt(i);
                                        if (alreadySent.contains(modechar)) {
                                            continue;
                                        } else {
                                            alreadySent.add(modechar);
                                        }
                                        final Collection<ChannelListModeItem> modeList = channel.getListMode(modechar);
                                        final ServerType st = ((IRCParser)myParser).getServerType();
                                        final boolean owner386 = ServerTypeGroup.OWNER_386.isMember(st);
                                        final boolean isFreenode = ServerTypeGroup.FREENODE.isMember(st);
                                        final boolean serverSupportsListmode = (myParser instanceof IRCParser) && ((IRCParser) myParser).get005().containsKey("LISTMODE");

                                        if (modeList != null) {
                                            // This covers most list items, if its not listed here it
                                            // gets forwarded to the server.
                                            boolean backwardsList = false;
                                            if (modechar == 'b') {
                                                itemNumber = 367;
                                                listName = "Channel Ban List";
                                            } else if (modechar == 'd') {
                                                itemNumber = 367;
                                                listName = "Channel Ban List";
                                            } else if (modechar == 'q' && !owner386) {
                                                itemNumber = 367;
                                                listName = "Channel Ban List";
                                            } else if (modechar == 'q' && owner386) {
                                                itemNumber = 387;
                                                backwardsList = true;
                                                listName = "Channel Owner List";
                                            } else if (modechar == 'a') {
                                                itemNumber = 389;
                                                backwardsList = true;
                                                listName = "Channel Protected List";
                                            } else if (modechar == 'e') {
                                                itemNumber = 348;
                                                listName = "Channel Exception List";
                                            } else if (modechar == 'I') {
                                                itemNumber = 346;
                                                listName = "Channel Invite List";
                                            } else if (modechar == 'R') {
                                                itemNumber = 344;
                                                listName = "Channel Reop List";
                                            } else if (modechar == 'w') {
                                                itemNumber = 910;
                                                listName = "Channel Access List";
                                            } else if (modechar == 'X') {
                                                itemNumber = 954;
                                                backwardsList = true;
                                                listName = "channel exemptchanops list";
                                            } else if (modechar == 'g') {
                                                itemNumber = 941;
                                                backwardsList = true;
                                                listName = "Channel spamfilter List";
                                            } else if (isListmode && !serverSupportsListmode) {
                                                // Well bugger. Client wants LISTMODE, we don't know how to handle the
                                                // mode ourself and the underlying server doesn't support it.
                                                // This is not ideal, so let the user know, then they can report it as
                                                // an issue if they wish.
                                                user.sendBotMessage("Unable to fulfil LISTMODE request for %s (Mode: %s) - This should be reported. See: https://github.com/ShaneMcC/DFBnc/issues", channel, modechar);
                                                continue;
                                            } else {
                                                // Erm, this won't work, just error instead.
                                                user.sendBotMessage("Unable to fulfil MODE request for %s (Mode: %s) - This should be reported.  See: https://github.com/ShaneMcC/DFBnc/issues", channel, modechar);
                                                continue;
                                            }
                                            String prefix = "";
                                            // TODO: This should probably use the parser ServerTypes
                                            if (isFreenode && modechar == 'q') {
                                                prefix = "%";
                                            }
                                            if (isListmode) {
                                                prefix = modechar + " " + prefix;
                                                itemNumber = 997;
                                                listName = "Channel List Modes";
                                            }
                                            for (ChannelListModeItem item : modeList) {
                                                user.sendIRCLine(itemNumber, myParser.getLocalClient().getNickname() + " " + channel, prefix + item.getItem() + " " + item.getOwner() + " " + item.getTime(), false);
                                            }
                                            if (!isListmode && (modechar == 'b' || modechar == 'q')) {
                                                // If we are emulating a hyperian ircd, we need to send these together, unless we are using listmode.
                                                if (isFreenode) {
                                                    Collection<ChannelListModeItem> newmodeList;
                                                    if (modechar == 'b') {
                                                        newmodeList = channel.getListMode('q');
                                                        alreadySent.add('q');
                                                    } else {
                                                        newmodeList = channel.getListMode('b');
                                                        alreadySent.add('b');
                                                    }

                                                    // This actually applies to the listmode being q, but the requested mode was b, so we check that
                                                    if (modechar == 'b') {
                                                        prefix = "%";
                                                    } else {
                                                        prefix = "";
                                                    }
                                                    for (ChannelListModeItem item : newmodeList) {
                                                        user.sendIRCLine(itemNumber, myParser.getLocalClient().getNickname() + " " + channel, prefix + item.getItem() + " " + item.getOwner() + " " + item.getTime(), false);
                                                    }
                                                }
                                            }
                                            if (!isListmode) {
                                                user.sendIRCLine(itemNumber + (backwardsList ? -1 : 1), myParser.getLocalClient().getNickname() + " " + channel, "End of " + listName + " (Cached)");
                                            }
                                        } else {
                                            if (outData.length() == 0) {
                                                outData.append(line[0].toUpperCase()).append(' ').append(channelName).append(' ');
                                            }
                                            outData.append(modeCharList.charAt(i));
                                            resetOutData = true;
                                        }
                                    }
                                    if (isListmode) {
                                        user.sendIRCLine(itemNumber + 1, myParser.getLocalClient().getNickname() + " " + channel, "End of " + listName + " (Cached)");
                                    }
                                } else {
                                    // This will only actually be 0 if we havn't recieved the initial
                                    // on-Join reply from the server.
                                    // In this case the actual 324 and the 329 from the server will get
                                    // through to the client
                                    if (((IRCChannelInfo) channel).getCreateTime() > 0) {
                                        user.sendIRCLine(324, myParser.getLocalClient().getNickname() + " " + channel, ((IRCChannelInfo) channel).getModes(), false);
                                        user.sendIRCLine(329, myParser.getLocalClient().getNickname() + " " + channel, "" + ((IRCChannelInfo) channel).getCreateTime(), false);
                                    } else {
                                        allowLine(channel, "324");
                                        allowLine(channel, "329");
                                    }
                                }
                            } else if (client == myParser.getLocalClient()) {
                                user.sendIRCLine(221, myParser.getLocalClient().getNickname(), ((IRCClientInfo) client).getModes(), false);
                            } else {
                                if (outData.length() == 0) {
                                    outData.append(line[0].toUpperCase()).append(' ');
                                } else {
                                    outData.append(',');
                                }
                                outData.append(channelName);
                            }
                        }
                    } else {
                        if (outData.length() == 0) {
                            outData.append(line[0].toUpperCase()).append(' ');
                        } else {
                            outData.append(',');
                        }
                        outData.append(channelName);
                    }
                }
            }
            if (outData.length() == 0) {
                return;
            }
        } else if (line[0].equalsIgnoreCase("quit")) {
            return;
        }

        if (outData.length() == 0) {
            myParser.sendRawMessage(data);
        } else {
            myParser.sendRawMessage(outData.toString());
        }
    }

    /**
     * Get the requeueList.
     * This is used by the requeueTimer, it returns a clone of the requeueList,
     * and then empties the requeueList.
     *
     * @return Clone of the requeueList
     */
    List<RequeueLine> getRequeueList() {
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
        if (line.length == 0 || line.length == 1) {
            user.sendIRCLine(Consts.ERR_NEEDMOREPARAMS, line[0], "Not enough parameters");
            return false;
        }

        if (line[1].equalsIgnoreCase("-f")) {
            // if (/topic -f #foo)
            if (line.length == 3 || line.length == 4) {
                outData.append(line[0]);
                outData.append(' ');
                outData.append(line[2]);
                if (line.length == 4) {
                    outData.append(line[0].equalsIgnoreCase("topic") ? " :" : " ");
                    outData.append(line[3]);
                }
                ChannelInfo channel = myParser.getChannel(line[2]);
                if (line[0].equalsIgnoreCase("topic")) {
                    allowLine(channel, "331");
                    allowLine(channel, "332");
                    allowLine(channel, "333");
                } else if (line[0].equalsIgnoreCase("names")) {
                    allowLine(channel, "353");
                    allowLine(channel, "366");
                } else if (line[0].equalsIgnoreCase("mode")) {
                    if (channel != null) {
                        if (line.length == 4) {
                            for (int i = 0; i < line[3].length(); ++i) {
                                char modechar = line[3].charAt(i);
                                Collection<ChannelListModeItem> modeList = channel.getListMode(modechar);
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

                                    if (((IRCParser)myParser).getServerType() == ServerType.INSPIRCD) {
                                        if (modechar == 'w') {
                                            allowLine(channel, "910");
                                            allowLine(channel, "911");
                                        } else if (modechar == 'g') {
                                            allowLine(channel, "940");
                                            allowLine(channel, "941");
                                        } else if (modechar == 'X') {
                                            allowLine(channel, "954");
                                            allowLine(channel, "953");
                                        }
                                    }

                                }
                            }
                        } else {
                            allowLine(channel, "324");
                            allowLine(channel, "329");
                        }
                    } else {
                        allowLine(null, "221");
                    }
                } else if (line[0].equalsIgnoreCase("listmode")) {
                    if (myParser instanceof IRCParser) {
                        final IRCParser ircParser = ((IRCParser) myParser);
                        if (ircParser.get005().containsKey("LISTMODE")) {
                            allowLine(channel, ircParser.get005().get("LISTMODE"));
                            allowLine(channel, ircParser.get005().get("LISTMODEEND"));
                        }
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
                return true;
            } else if (line[0].equalsIgnoreCase("topic")) {
                outData.append(line[0]);
                outData.append(" ");
                outData.append(line[1]);
                outData.append(" :");
                outData.append(line[2]);
            }
        } else {
            outData.append(line[0]);
            for (int i = 1; i < line.length; i++) {
                outData.append(" ");
                outData.append(line[i]);
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getServerName() {
        if (parserReady) {
            return myParser.getServerName();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNickChanged(final Parser parser, final Date date, final ClientInfo client, final String oldNick) {
        if (client == parser.getLocalClient()) {
            for (UserSocket socket : myAccount.getUserSockets()) {
                socket.setNickname(parser.getLocalClient().getNickname());
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
            tokens = (List<String>) ((IRCChannelInfo) channel).getMap().get("AllowedTokens");
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
            ((IRCChannelInfo) channel).getMap().put("AllowedTokens", tokens);
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
            tokens = (List<String>) ((IRCChannelInfo) channel).getMap().get("AllowedTokens");
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
            ((IRCChannelInfo) channel).getMap().put("AllowedTokens", tokens);
        }
    }

    /**
     * Check if a line is temporarily allowed.
     *
     * @param channel Channel to check line allowance for (or null for the global list)
     * @param token token to check
     * @return True if this line is allowed, else false
     */
    @SuppressWarnings("unchecked")
    private boolean checkAllowLine(final ChannelInfo channel, final String token) {
        List<String> tokens;
        if (channel != null) {
            tokens = (List<String>) ((IRCChannelInfo) channel).getMap().get("AllowedTokens");
            if (tokens == null) {
                tokens = new ArrayList<String>();
            }
        } else {
            tokens = allowTokens;
        }

        return tokens.contains(token);
    }

    /** {@inheritDoc} */
    @Override
    public void onChannelSelfJoin(final Parser parser, final Date date, final ChannelInfo channel) {
        // Allow Names Through
        allowLine(channel, "353");
        allowLine(channel, "366");
        // Allow Topic Through
        allowLine(channel, "331");
        allowLine(channel, "332");
        allowLine(channel, "333");

        myAccount.getConfig().addListener(this);
        channel.getMap().put("backbufferList", new RollingList<BackbufferMessage>(myAccount.getConfig().getIntOption("server", "backbuffer", 0)));

        // Fake a join.
        onChannelJoin(parser, date, channel, channel.getChannelClient(parser.getLocalClient()));
    }

    /** {@inheritDoc} */
    @Override
    public void onChannelJoin(final Parser parser, final Date date, final ChannelInfo channel, final ChannelClientInfo client) {
        // Fake a join to connected clients.
        // We do this rather than passing the "JOIN" through in onDataIn so that
        // we can deal with "extended-join" where possible.
        final ClientInfo ci = client.getClient();
        final String accountName = (ci instanceof IRCClientInfo) ? ((IRCClientInfo)ci).getAccountName() : "*";

        for (UserSocket socket : myAccount.getUserSockets()) {
            if (socket.syncCompleted()) {
                if (!socket.allowedChannel(channel.getName())) { continue; }

                if (socket.getCapabilityState("extended-join") == CapabilityState.ENABLED) {
                    socket.sendLine(":%s JOIN %s %s :%s", ci.toString(), channel.getName(), accountName, ci.getRealname());
                } else {
                    socket.sendLine(":%s JOIN %s", ci.toString(), channel.getName());
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public void configChanged(final String domain, final String setting) {
        if (domain.equalsIgnoreCase("server") && setting.equalsIgnoreCase("backbuffer")) {
            final int size = myAccount.getConfig().getIntOption("server", "backbuffer", 0);
            for (ChannelInfo channel : myParser.getChannels()) {
                final RollingList<BackbufferMessage> myList = (RollingList<BackbufferMessage>)channel.getMap().get("backbufferList");
                myList.setCapacity(size);
            }
        }
    }

    /**
     * Add a message to the backbuffer.
     *
     * @param time
     * @param message
     */
    @SuppressWarnings("unchecked")
    private void addBackbufferMessage(final ChannelInfo channel, final long time, final String message) {
        if (channel != null) {
            final RollingList<BackbufferMessage> myList = (RollingList<BackbufferMessage>)channel.getMap().get("backbufferList");
            myList.add(new BackbufferMessage(time, message));
        }
    }

    /** {@inheritDoc} */
    @Override
    public RollingList<BackbufferMessage> getBackbufferList(final String channel) {
        final ChannelInfo ci = myParser.getChannel(channel);
        return getBackbufferList(ci);
    }

    /**
     * Get backbuffer for a given channel.
     *
     * @param ci ChannelInfo to get backbuffer from
     * @return backbuffer for the channel
     */
    @SuppressWarnings("unchecked")
    public RollingList<BackbufferMessage> getBackbufferList(final ChannelInfo ci) {
        if (ci != null) {
            final RollingList<BackbufferMessage> list = (RollingList<BackbufferMessage>)ci.getMap().get("backbufferList");
            if (list != null) {
                return list;
            }
        }

        return new RollingList<BackbufferMessage>(0);
    }

    /** {@inheritDoc} */
    @Override
    public void onDataOut(final Parser parser, final Date date, final String data, final boolean fromParser) {
        final String[] bits = IRCParser.tokeniseLine(data);
        if (bits[0].equals("PRIVMSG") && bits.length > 1) {
            final ChannelInfo channel = parser.getChannel(bits[1]);
            if (channel != null) {
                this.addBackbufferMessage(channel, System.currentTimeMillis(), String.format(":%s %s", this.getMyHost(), data));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onDataIn(final Parser parser, final Date date, final String data) {
        boolean forwardLine = true;
        String channelName = null;
        final String[] bits = IRCParser.tokeniseLine(data);
        // Don't forward pings or pongs from the server
        if (bits[1].equals("PONG") || bits[0].equals("PONG") || bits[1].equals("PING") || bits[0].equals("PING")) {
            return;
        }

        // Don't forward CAP from the server
        if (bits[1].equals("CAP")) { return; }

        // Don't forward JOINs from the server (We fake them in the appropriate
        // onchannel(self)join callbacks - we need the parser to process them
        // first.
        if (bits[1].equals("JOIN")) { return; }

        if (bits[1].equals("PRIVMSG")) {
            final ChannelInfo channel = parser.getChannel(bits[2]);
            if (channel != null) {
                channelName = channel.getName();
                this.addBackbufferMessage(channel, System.currentTimeMillis(), data);
            }
        } else if (parser.isValidChannelName(bits[2])) {
            channelName = bits[2];
        }

        try {
            final int numeric = Integer.parseInt(bits[1]);
            if (myParser instanceof IRCParser) {
                final IRCParser ircParser = ((IRCParser) myParser);
                final boolean supportLISTMODE = ircParser.get005().containsKey("LISTMODE");
                if (supportLISTMODE) {
                    if (bits[1].equals(ircParser.get005().get("LISTMODE"))) {
                        return;
                    } else if (bits[1].equals(ircParser.get005().get("LISTMODEEND"))) {
                        return;
                    }
                }
            }
            final ChannelInfo channel = (bits.length > 3) ? myParser.getChannel(bits[3]) : null;
            if (channel != null) {
                channelName = channel.getName();
            }
            switch (numeric) {
                case 324: // Channel Modes
                case 332: // Topic
                case 367: // Ban List
                case 348: // Exception List
                case 346: // Invite List
                case 387: // Owner List
                case 389: // Protected List
                case 344: // Reop List
                case 910: // Access List
                case 954: // ExemptChanOps List
                case 941: // Spamfilter List
                    forwardLine = checkAllowLine(channel, bits[1]);
                    break;

                case 353: // Names
                    channelName = bits[4];
                    forwardLine = checkAllowLine(myParser.getChannel(bits[4]), bits[1]);
                    break;

                case 368: // Ban List End
                case 349: // Exception List End
                case 347: // Invite List End
                case 345: // Reop List
                case 911: // Reop List
                    forwardLine = checkAllowLine(channel, bits[1]);
                    if (forwardLine) {
                        disallowLine(channel, bits[1]);
                        disallowLine(channel, Integer.toString(numeric - 1));
                    }
                    break;

                case 386: // Owner List (Backwards List)
                case 388: // Protected List (Backwards List)
                case 953: // ExemptChanOps List (Backwards List)
                case 940: // Spamfilter List (Backwards List)
                    forwardLine = checkAllowLine(channel, bits[1]);
                    if (forwardLine) {
                        disallowLine(channel, bits[1]);
                        disallowLine(channel, Integer.toString(numeric + 1));
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
        } catch (final NumberFormatException nfe) {
            /* Non-Numeric Line. */
        }

        if (forwardLine) {
            for (UserSocket socket : myAccount.getUserSockets()) {
                if (socket.syncCompleted()) {
                    if (channelName != null && !socket.allowedChannel(channelName)) { continue; }
                    socket.sendLine(data);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onServerReady(final Parser parser, final Date date) {
        //hasPost005 = true;
        // We no longer need this callback, so lets remove it
        myParser.getCallbackManager().delCallback(NumericListener.class, this);
    }

    /** {@inheritDoc} */
    @Override
    public void onMOTDEnd(final Parser parser, final Date date, final boolean noMOTD, final String data) {
        hasMOTDEnd = true;
        List<String> myList = new ArrayList<String>();
        myList = myAccount.getConfig().getListOption("irc", "perform.connect", myList);
        Logger.debug3("Connected. Handling performs");
        for (String line : myList) {
            myParser.sendRawMessage(filterPerformLine(line));
            Logger.debug3("Sending perform line: " + line);
        }
        if (myAccount.getUserSockets().isEmpty()) {
            myList = new ArrayList<String>();
            myList = myAccount.getConfig().getListOption("irc", "perform.lastdetach", myList);
            for (String line : myList) {
                myParser.sendRawMessage(filterPerformLine(line));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNumeric(final Parser parser, final Date date, final int numeric, final String[] token) {
        if (numeric > 1 && numeric < 6) {
            if (numeric == 5 && !hacked005) {
                // Add our own 005.
                // * Show support for advanced LISTMODE (http://shane.dmdirc.com/listmodes.php)
                // * Show that this is a BNC Connection
                final String my005 = ":" + getServerName() + " 005 " + myParser.getLocalClient().getNickname() + " LISTMODE=997 BNC=DFBNC :are supported by this server";
                final String ts005 = ":" + getServerName() + " 005 " + myParser.getLocalClient().getNickname() + " TIMESTAMPEDIRC :are supported by this server";

                for (UserSocket socket : myAccount.getUserSockets()) {
                    socket.sendLine(my005);

                    // Allow support for old-style TSIRC if it isn't already
                    // enabled.
                    if (socket.getCapabilityState("dfbnc.com/tsirc") != CapabilityState.ENABLED) {
                        socket.sendLine(ts005);
                    }
                }
                connectionLines.add(my005);
                hacked005 = true;
            }
            connectionLines.add(parser.getLastLine());
        }
        // The parser no longer has separate calls before and after 005..
        if (numeric == 1) {
            parserReady = true;
            for (UserSocket socket : myAccount.getUserSockets()) {
                socket.setPost001(true);
                socket.setNickname(parser.getLocalClient().getNickname());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onSocketClosed(final Parser parser, final Date date) {
        requeueTimer.cancel();
        myAccount.handlerDisconnected("Remote connection closed.");
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectError(final Parser parser, final Date date, final ParserError errorInfo) {
        String description;
        if (errorInfo.getException() == null) {
            description = errorInfo.getData();
        } else {
            final Exception exception = errorInfo.getException();
            if (exception instanceof java.net.UnknownHostException) {
                description = "Unknown host (unable to resolve)";
            } else if (exception instanceof java.net.NoRouteToHostException) {
                description = "No route to host";
            } else if (exception instanceof java.net.SocketTimeoutException) {
                description = "Connection attempt timed out";
            } else if (exception instanceof java.net.SocketException || exception instanceof javax.net.ssl.SSLException) {
                description = exception.getMessage();
            } else {
                description = "Unknown error: " + exception.getMessage();
            }
        }
        requeueTimer.cancel();
        myAccount.handlerDisconnected("Connection error: " + description);
    }


    /** {@inheritDoc} */
    @Override
    public void onErrorInfo(final Parser parser, final Date date, final ParserError errorInfo) {
        Logger.error("Parser error occurred: " + errorInfo.getData());
        Logger.error("\tLast line received: " + errorInfo.getLastLine());
        errorInfo.getException().printStackTrace();

        if (myAccount.getConfig().getBoolOption("server", "reporterrors", false)) {
            myAccount.sendBotMessage("Parser error occurred: %s", errorInfo.getData());
            myAccount.sendBotMessage("    Last line received: %s", errorInfo.getLastLine());
        }

        myAccount.reportException(errorInfo.getException());
    }

    /** {@inheritDoc} */
    @Override
    public void userConnected(final UserSocket user) {
        Logger.debug2("IRC userConnected: Check for 001: " + parserReady);
        // If the parser has processed a 001, we need to send our own
        if (parserReady) {
            Logger.debug2("Has 001");
            user.sendIRCLine(1, myParser.getLocalClient().getNickname(), "Welcome to the Internet Relay Network, " + myParser.getLocalClient().getNickname());
            user.setNickname(myParser.getLocalClient().getNickname());
            // Now send any of the 002-005 lines that we have
            for (String line : connectionLines) {
                final String[] bits = line.split(" ");
                if (bits.length > 2) {
                    bits[2] = user.getNickname();
                    user.sendLine(Util.joinString(bits, " ", 0, 0));
                }
            }
            // And hack in a tsirc 005 if TSIRC has not already been enabled
            // with CAP.
            if (user.getCapabilityState("dfbnc.com/tsirc") != CapabilityState.ENABLED) {
                user.sendLine(":" + getServerName() + " 005 " + user.getNickname() + " TIMESTAMPEDIRC :are supported by this server");
            }
            user.setPost001(true);
            // Now, if the parser has recieved an end of MOTD Line, we should send our own MOTD and User Host info
            if (hasMOTDEnd) {
                user.sendIRCLine(375, myParser.getLocalClient().getNickname(), "- " + myParser.getServerName() + " Message of the Day -");
                user.sendIRCLine(372, myParser.getLocalClient().getNickname(), "You are connected to an IRC Server, please type /MOTD to get the server's MOTD.");
                user.sendIRCLine(376, myParser.getLocalClient().getNickname(), "End of /MOTD command.");

                // Now send 302 to let the client know its userhost
                // also send a 306 if the user is away so that the client can update itself
                final ClientInfo me = myParser.getLocalClient();
                StringBuilder str302 = new StringBuilder(me.getNickname());
                if (((IRCClientInfo) me).isOper()) {
                    str302.append('*');
                }
                str302.append('=');
                if (((IRCClientInfo) me).getAwayState() == AwayState.AWAY) {
                    user.sendIRCLine(306, myParser.getLocalClient().getNickname(), "You have been marked as being away");
                    str302.append('-');
                } else {
                    str302.append('+');
                }
                str302.append(me.getUsername()).append('@').append(me.getHostname());
                user.sendIRCLine(302, myParser.getLocalClient().getNickname(), str302.toString());
                // Now send the usermode info
                user.sendIRCLine(221, myParser.getLocalClient().getNickname(), ((IRCClientInfo) me).getModes(), false);

                final Collection<? extends ChannelInfo> channels = myParser.getChannels();

                new Timer().schedule(new TimerTask() {
                    /** {@inheritDoc} */
                    @Override
                    public void run() {
                        if (!user.getSocketWrapper().isConnected()) { return; }

                        for (final ChannelInfo channel : channels) {
                            if (!user.allowedChannel(channel.getName())) { continue; }

                            if (user.getCapabilityState("extended-join") == CapabilityState.ENABLED) {
                                final String accountName = (me instanceof IRCClientInfo) ? ((IRCClientInfo)me).getAccountName() : "*";
                                user.sendLine(":%s JOIN %s %s :%s", me, channel, accountName, me.getRealname());
                            } else {
                                user.sendLine(":%s JOIN %s", me, channel);
                            }

                            sendTopic(user, channel);
                            sendNames(user, channel);

                            if (myAccount.getConfig().getIntOption("server", "backbuffer", 0) > 0) {
                                sendBackbuffer(user, channel);
                            }
                        }
                        user.setSyncCompleted();

                        if (myAccount.getUserSockets().size() == 1) {
                            List<String> myList = new ArrayList<String>();
                            myList = myAccount.getConfig().getListOption("irc", "perform.firstattach", myList);
                            for (String line : myList) {
                                myParser.sendRawMessage(filterPerformLine(line));
                            }
                        }
                    }
                }, 1500);
            }
        } else {
            // Make sure cliets get marked as sync completed.
            user.setSyncCompleted();
        }
        Logger.debug2("end irc user connected.");
    }

    /**
     * Send a topic reply for a channel to the given user
     *
     * @param user User to send reply to
     * @param channel Channel to send reply for
     */
    public void sendTopic(final UserSocket user, final ChannelInfo channel) {
        if (!channel.getTopic().isEmpty()) {
            user.sendIRCLine(332, myParser.getLocalClient().getNickname() + " " + channel, channel.getTopic());
            user.sendIRCLine(333, myParser.getLocalClient().getNickname() + " " + channel, channel.getTopicSetter() + " " + channel.getTopicTime(), false);
        } else {
            user.sendIRCLine(331, myParser.getLocalClient().getNickname() + " " + channel, "No topic is set.");
        }
    }

    /**
     * Send a names reply for a channel to the given user
     *
     * @param user User to send reply to
     * @param channel Channel to send reply for
     */
    public void sendNames(final UserSocket user, final ChannelInfo channel) {
        final int maxLength = 500 - (":" + getServerName() + " 353 " + myParser.getLocalClient(). getNickname() + " = " + channel + " :").length();
        final StringBuilder names = new StringBuilder();
        final StringBuilder name = new StringBuilder();
        for (ChannelClientInfo cci : channel.getChannelClients()) {
            name.setLength(0);
            if (user.getCapabilityState("multi-prefix") == CapabilityState.ENABLED && cci instanceof IRCChannelClientInfo) {
                name.append(((IRCChannelClientInfo)cci).getChanModeStr(true));
            } else {
                name.append(cci.getImportantModePrefix());
            }
            if (user.getCapabilityState("userhost-in-names") == CapabilityState.ENABLED) {
                name.append(cci.getClient().toString());
            } else {
                name.append(cci.getClient().getNickname());
            }

            if (name.toString().length() > (maxLength - names.length())) {
                user.sendIRCLine(353, myParser.getLocalClient().getNickname() + " = " + channel, names.toString().trim());
                names.setLength(0);
            }
            names.append(name.toString()).append(" ");
        }
        if (names.length() > 0) {
            user.sendIRCLine(353, myParser.getLocalClient().getNickname() + " = " + channel, names.toString().trim());
        }
        user.sendIRCLine(366, myParser.getLocalClient().getNickname() + " " + channel, "End of /NAMES list. (Cached)");
    }

    /**
     * Send the current backbuffer for a given channel to the given user.
     *
     * @param user User to send backbuffer to
     * @param channel Channel to send backbuffer for
     */
    public void sendBackbuffer(final UserSocket user, final ChannelInfo channel) {
        final RollingList<BackbufferMessage> backbufferList = getBackbufferList(channel);
        sendBackbuffer(user, channel, backbufferList);
    }

    /**
     * Send the given backbuffer to the given channel to the given user.
     *
     * @param user User to send backbuffer to
     * @param channel Channel to send backbuffer to
     * @param backbufferList Backbuffer to send
     */
    private void sendBackbuffer(final UserSocket user, final ChannelInfo channel, final RollingList<BackbufferMessage> backbufferList) {
        if (backbufferList.isEmpty()) {
            user.sendBotChat(channel.getName(), "NOTICE", "This channel has no current backbuffer.");
            return;
        }

        user.sendBotChat(channel.getName(), "NOTICE", "Beginning backbuffer...");
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (BackbufferMessage message : backbufferList) {
            final String line;

            if (user.getCapabilityState("dfbnc.com/tsirc") == CapabilityState.ENABLED) {
                line = "@" + Long.toString(message.getTime()) + "@" + message.getMessage();
            } else {
                final String date = "    [" + sdf.format(message.getTime()) + "]";

                // If it's a CTCP (like an action), insert the timestamp before
                // the trailing 0x01
                if (message.getMessage().endsWith("\001")) {
                    line = message.getMessage().substring(0, message.getMessage().length() - 1) + date + "\001";
                } else {
                    line = message.getMessage() + date;
                }
            }

            final int maxLength = 510;

            if (line.length() <= maxLength) {
                user.sendLine(line);
            } else {
                // Line is longer than 510...
                // We need to split it and send it in bits.

                // Firstly separate the protocol bits, and the acual message
                final int lastarg = line.indexOf(" :");
                final String lastBit = line.substring(lastarg + 2);
                final String startBits = line.substring(0, lastarg) + " :";

                // Now work out the allowed characters per bit.
                final int allowed = maxLength - startBits.length();

                StringBuilder sendLine = new StringBuilder(startBits);

                for (int i = 0; i < lastBit.length(); i += allowed) {
                    sendLine.append(lastBit.substring(i, Math.min(i + allowed, lastBit.length())));
                    user.sendLine(sendLine.toString());
                    sendLine = new StringBuilder(startBits);
                }
            }
        }
        user.sendBotChat(channel.getName(), "NOTICE", "End of backbuffer.");
    }

    /**
     * Called when a UserSocket is closed on an account that this class is
     * linked to.
     *
     * @param user UserSocket for user
     */
    @Override
    public void userDisconnected(final UserSocket user) {
        if (parserReady) {
            if (myAccount.getUserSockets().isEmpty()) {
                List<String> myList = new ArrayList<String>();
                myList = myAccount.getConfig().getListOption("irc", "perform.lastdetach", myList);
                for (String line : myList) {
                    myParser.sendRawMessage(filterPerformLine(line));
                }
            }
        }
    }

    /**
     * Filter a perform line and return the line after substitutions have occured
     *
     * @param input Line to filter
     *
     * @return Processed line
     */
    public String filterPerformLine(final String input) {
        String result = input;
        result = result.replaceAll("$me", myParser.getLocalClient().getNickname());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void cleanupUser(final UserSocket user, final String reason) {
        for (ChannelInfo channel : myParser.getChannels()) {
            /*
            user.sendLine(":%s!bot@%s JOIN %s", Util.getBotName(), Util.getServerName(myAccount), channel.getName());
            user.sendLine(":%s!bot@%s MODE %s +o %1$s", Util.getBotName(), Util.getServerName(myAccount), channel.getName());
            user.sendBotChat(channel.getName(), "PRIVMSG", "Disconnected from server: "+reason);
            for (ChannelClientInfo cci : channel.getChannelClients()) {
                user.sendLine(":%s!bot@%s KICK %s %s :Socket Closed: %s", Util.getBotName(), Util.getServerName(myAccount), channel.getName(), cci.getClient().getNickname(), reason);
            }
            user.sendLine(":%s!bot@%s PART %s :My work here is done...", Util.getBotName(), Util.getServerName(myAccount), channel.getName());
            */

            user.sendLine(":%s KICK %s %s :Socket Closed: %s", myAccount.getServerName(), channel.getName(), user.getNickname(), reason);
        }
    }

    /**
     * Is this socket allowed to interact with the given channel name?
     *
     * @param channel Channel Name
     * @return True if this socket is allowed, else false.
     */
    @Override
    public boolean allowedChannel(final UserSocket user, final String channel) {
        // TODO: This whole method needs optimising really... Allowed channels
        //       need caching etc.
        final List<String> validChannelList = user.getAccount().getConfig().getListOption("irc",  "channelwhitelist." + user.getClientID(), new ArrayList<String>());

        if (validChannelList.isEmpty()) {
            return true;
        } else {
            for (final String c : validChannelList) {
                if (c.equalsIgnoreCase(channel)) {
                    return true;
                }
            }
        }

        return false;
    }

}
/**
 * This stores a line that is being requeued.
 */
class RequeueLine {

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
            connectionHandler.processDataRecieved(user, line, IRCParser.tokeniseLine(line), times + 1);
        }
    }
}

/**
 * This takes items from the requeue list, and requeues them.
 */
class RequeueTimerTask extends TimerTask {

    /** The IRCConnectionHandler that owns this task */
    final IRCConnectionHandler connectionHandler;

    /**
     * Create a new RequeueTimerTask
     *
     * @param connectionHandler Parent connection handler
     */
    public RequeueTimerTask(final IRCConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    /** Actually do stuff */
    @Override
    public void run() {
        List<RequeueLine> list = connectionHandler.getRequeueList();
        for (RequeueLine line : list) {
            line.reprocess(connectionHandler);
        }
    }
}

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
package com.dfbnc.sockets;

import com.dmdirc.parser.irc.CapabilityState;
import com.dmdirc.parser.irc.IRCParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import com.dfbnc.Account;
import com.dfbnc.AccountManager;
import com.dfbnc.ConnectionHandler;
import com.dfbnc.Consts;
import com.dfbnc.DFBnc;
import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandNotFoundException;
import uk.org.dataforce.libs.logger.Logger;
import com.dfbnc.util.Util;

/**
 * This socket handles actual clients connected to the bnc.
 */
public class UserSocket extends ConnectedSocket {
    /** Known sockets are referenced in this HashMap. */
    private final static HashMap<String, UserSocket> knownSockets = new HashMap<>();

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

    /** Has the sync for this user been run? */
    private boolean syncCompleted = false;

    /** Has this socket been announced to other users? */
    private boolean socketAnnouncement = false;

    /** Given username */
    private String username = null;
    /** Given clientID */
    private String clientID = null;
    /** Given client version */
    private String clientVersion = null;
    /** Given realname */
    private String realname = null;
    /** Given nickname (post-authentication this is the nickname the client knows itself as) */
    private String nickname = null;
    /** Given password */
    private String password = null;
    /** Password attempts */
    private int passwordTries = 0;
    /** Counter for inactivity. */
    private int inactiveCounter = 0;

    /** Maximum password attempts.
     * This should be changed to a config setting at some point.
     */
    private final int maxPasswordTries = 3;

    /** IP Address of this socket */
    private String myIP = "0.0.0.0";

    /** The Account object for this connect (This is null before authentication) */
    private Account myAccount = null;

    /** Is closeAll being run? (This prevents socketClosed removing the HashMap entry) */
    private static boolean closeAll = false;

    /** Is this socket in the middle of capability negotiation? */
    private boolean isNegotiating = false;

    /** Lines buffered during negotiation. */
    private List<String> negotiationLines = new LinkedList<String>();

    /** Map of capabilities and their state. */
    private final Map<String, CapabilityState> capabilities = new HashMap<String, CapabilityState>();

    /** Map of objects associated with this UserSocket. */
    private final static HashMap<Object, Object> myMap = new HashMap<Object, Object>();

    /**
     * Create a new UserSocket.
     *
     * @param sChannel Socket to control
     * @param fromSSL Did this come from an SSL ListenSocket ?
     * @throws IOException If there is a problem setting up the socket.
     */
    public UserSocket(final SocketChannel sChannel, final boolean fromSSL) throws IOException {
        super(sChannel, "[UserSocket "+sChannel+"]", fromSSL);
        synchronized (knownSockets) {
            final Random random = new Random();
            final StringBuilder tempid = new StringBuilder(String.valueOf(random.nextInt(10)));

            while (knownSockets.containsKey(tempid.toString())) {
                tempid.append(random.nextInt(10));
            }

            myID = tempid.toString();
            knownSockets.put(myID, this);
        }

        super.setSocketID("[UserSocket: " + myID + "]");

        final InetSocketAddress remoteAddress = (InetSocketAddress)mySocketWrapper.getRemoteSocketAddress();
        final String remoteInfo = "[" + remoteAddress.getAddress() + "]:" + remoteAddress.getPort();
        final InetSocketAddress localAddress = (InetSocketAddress)mySocketWrapper.getLocalSocketAddress();
        final String localInfo = "[" + localAddress.getAddress() + "]:" + localAddress.getPort();
        if (fromSSL) {
            myInfo = remoteInfo+" (" + localInfo + " [SSL]) [" + myID + "]";
        } else {
            myInfo = remoteInfo+" (" + localInfo + ") [" + myID + "]";
        }

        myIP = remoteAddress.getAddress().getHostAddress();
        Logger.info("User Connected: " + myInfo);

        // Set default capabilities
        synchronized (capabilities) {
            capabilities.put("userhost-in-names", CapabilityState.DISABLED);
            capabilities.put("multi-prefix", CapabilityState.DISABLED);
            capabilities.put("extended-join", CapabilityState.DISABLED);
            capabilities.put("dfbnc.com/tsirc", CapabilityState.DISABLED);
            capabilities.put("server-time", CapabilityState.DISABLED);
            capabilities.put("batch", CapabilityState.DISABLED);
            capabilities.put("dfbnc.com/channelhistory", CapabilityState.DISABLED);
        }
    }

    /**
     * Get the Server name that the BNC should use when dealing with this socket.
     * We might not always have an account to ask, so if not we will use the
     * default name.
     *
     * @return Server name that the BNC Uses
     */
    public String getServerName() {
        return (myAccount != null) ? myAccount.getServerName() : DFBnc.getBNC().getDefaultServerName();
    }

    /**
     * Get the client ID for this socket.
     *
     * @return The client ID for this socket.
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * Get the client Version for this socket.
     *
     * @return The client Version for this socket.
     */
    public String getClientVersion() {
        return clientVersion;
    }

    /**
     * Has this socket been announced to other connected users yet?
     *
     * @return True if other users know about this socket yet.
     */
    public boolean getSocketAnnouncement() {
        return socketAnnouncement;
    }

    /**
     * Set if this socket has been announced to other connected users
     *
     * @param newValue new value for the socketAnnouncement variable.
     */
    public void setSocketAnnouncement(final boolean newValue) {
        socketAnnouncement = newValue;
    }

    /**
     * Get the IP address of this socket
     *
     * @return IP Address of this socket.
     */
    public String getIP() {
        if (isSSL) {
            return '@' + myIP;
        } else {
            return myIP;
        }
    }

    /**
     * Get the info for this socket
     *
     * @return Info for this socket.
     */
    public String getInfo() {
        return myInfo;
    }

    /**
     * Get the socket ID for this socket.
     *
     * @return ID for this socket.
     */
    public String getID() {
        return myID;
    }

    /**
     * Check the state of the requested capability.
     *
     * @return State of the requested capability.
     */
    public CapabilityState getCapabilityState(final String capability) {
        synchronized (capabilities) {
            if (capabilities.containsKey(capability.toLowerCase())) {
                return capabilities.get(capability.toLowerCase());
            } else {
                return CapabilityState.INVALID;
            }
        }
    }

    /**
     * Set the state of the requested capability.
     *
     * @param capability Requested capability
     * @param state State to set for capability
     */
    public void setCapabilityState(final String capability, final CapabilityState state) {
        synchronized (capabilities) {
            if (capabilities.containsKey(capability.toLowerCase())) {
                capabilities.put(capability.toLowerCase(), state);
            }
        }
    }

    /**
     * Retrieves a {@link Map} which can be used to store arbitrary data
     * about the user socket.
     *
     * @return A map used for storing arbitrary data
     */
    public Map<Object, Object> getMap() {
        return myMap;
    }

    /**
     * Has this socket been synced?
     *
     * @return true if this socket has been synced.
     */
    public boolean syncCompleted() {
        return syncCompleted;
    }

    /**
     * Set this socket as syncCompleted.
     */
    public void setSyncCompleted() {
        syncCompleted = true;
    }

    /**
     * Get a usersocket by ID
     *
     * @param id Id of socket to get
     * @return Socket with the given ID if found.
     */
    public static UserSocket getUserSocket(final String id) {
        return knownSockets.get(id);
    }

    /**
     * Get a List of all UserSockets.
     *
     * @param account Account to check sockets against
     * @return a Collection of all UserSockets that are part of the given account
     */
    public static List<UserSocket> getUserSockets() {
        return new ArrayList<UserSocket>(knownSockets.values());
    }

    /**
     * Get a List of all UserSockets that are part of a given account
     *
     * @param account Account to check sockets against
     * @return a Collection of all UserSockets that are part of the given account
     */
    public static List<UserSocket> getUserSockets(final Account account) {
        final ArrayList<UserSocket> list = new ArrayList<UserSocket>();
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
                socket.close(reason);
            }
        }

        closeAll = false;
    }

    /**
     * Check all user sockets to make sure they are still active.
     *
     * What this actually does, is increment the "inactiveCount" variable.
     * Any input from the socket will reset it.
     *
     * The first time the counter goes above the threshold, a ping will be sent
     * to try and generate a response from the client. If the count reaches
     * threshold * 2, then the socket will be closed.
     *
     * If threshold is less than 1, then this will do nothing at all.
     *
     * @param threshold Threshold for killing sockets.
     */
    public static void checkAll(final int threshold) {
        if (threshold < 1) { return; }
        for (UserSocket socket : getUserSockets()) {
            if (++socket.inactiveCounter >= threshold) {
                if (socket.inactiveCounter == threshold) {
                    socket.sendLine("PING :%d", System.currentTimeMillis());
                } else if (socket.inactiveCounter > threshold * 2) {
                    socket.close("Socket inactivity counter threshold exceeded. (" + socket.inactiveCounter + " > " + threshold * 2 + ")");
                }
            }
        }
    }

    /**
     * Used to close this socket, and send a reason to the user.
     *
     * @param reason Reason for closing the socket.
     */
    public void close(final String reason) {
        this.sendLine(":%s NOTICE :Connection terminating (%s)", getServerName(), reason);
        this.closeSocket(reason);
    }

    /** {@inheritDoc} */
    @Override
    public void socketOpened() {
        sendBotMessage("Welcome to DFBnc (" + DFBnc.getVersion() + ")");
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
    public Account getAccount() {
        return myAccount;
    }

    /**
     * Get the realname supplied to this socket
     *
     * @return Realname supplied to this socket
     */
    public String getRealname() {
        return realname;
    }

    /**
     * Get the nickname for this socket
     *
     * @return nickname for this socket
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Set the nickname for this socket
     *
     * @param newValue New nickname
     */
    public void setNickname(final String newValue) {
        nickname = newValue;
    }

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
                } else if (method.equalsIgnoreCase("AUTH")) {
                    sendLine("NOTICE AUTH :- %s", String.format(data, args));
                } else {
                    sendBotLine(method, data, args);
                }
            } else {
                sendServerLine("NOTICE", data, args);
            }
        } else {
            sendLine("NOTICE AUTH :- %s", String.format(data, args));
        }
    }

    /**
     * Send a message from the bot to the given target.
     * This allows us to make the bot chat in channels
     *
     * @param target Target of message
     * @param type Type of message
     * @param data The format string
     * @param args The args for the format string
     */
    public void sendBotChat(final String target, final String type, final String data, final Object... args) {
        sendLine(":%s!bot@%s %s %s :%s", Util.getBotName(), getServerName(), type, target, String.format(data, args));
    }

    /**
     * Get the status of post001
     *
     * @return True if this socket has had a 001 sent to it, else false
     */
    public boolean getPost001() {
        return post001;
    }

    /**
     * Get the status of post001
     *
     * @param newValue new value for post001, True if this socket has had a 001 sent to it, else false
     */
    public void setPost001(final boolean newValue) {
        post001 = newValue;
    }

    /**
     * Is this socket allowed to interact with the given channel name?
     *
     * @param channel Channel Name
     * @return True if this socket is allowed, else false.
     */
    public boolean allowedChannel(final String channel) {
        return (myAccount.getConnectionHandler() != null) ? myAccount.getConnectionHandler().allowedChannel(this, channel) : true;
    }

    /**
     * Send a given raw line to all sockets
     *
     * @param line Line to send
     * @param ignoreThis Don't send the line to this socket if true
     */
    public void sendAll(final String line, final boolean ignoreThis) {
        for (UserSocket socket : this.getAccount().getUserSockets()) {
            if (ignoreThis && socket == this) {
                continue;
            }

            socket.sendLine(line);
        }
    }

    /**
     * Send a given raw line to all sockets, checking if the given channel
     * is allowed for that socket.
     *
     * @param channel Channel to check
     * @param line Line to send
     * @param ignoreThis Don't send the line to this socket if true
     */
    public void sendAllChannel(final String channel, final String line, final boolean ignoreThis) {
        for (UserSocket socket : this.getAccount().getUserSockets()) {
            if (ignoreThis && socket == this || !socket.allowedChannel(channel)) {
                continue;
            }

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
        sendLine(":%s!bot@%s %s %s :%s", Util.getBotName(), getServerName(), type, nickname, String.format(data, args));
    }

    /**
     * Send a message to the user from the bnc server in printf format.
     *
     * @param type the Type of message to send
     * @param data The format string
     * @param args The args for the format string
     */
    public void sendServerLine(final String type, final String data, final Object... args) {
        sendLine(":%s %s %s :%s", getServerName(), type, nickname, String.format(data, args));
    }

    /** {@inheritDoc} */
    @Override
    protected void socketClosed(final boolean userRequested) {
        if (!closeAll) {
            synchronized (knownSockets) {
                knownSockets.remove(myID);
            }
        }

        Logger.info("User Disconnected: " + myInfo);

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
            if (newLine.length > 0) {
                sendIRCLine(Consts.ERR_NEEDMOREPARAMS, newLine[0], "Not enough parameters");
            }
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void processLine(final String line) {
        // Reset the inactive counter.
        this.inactiveCounter = 0;

        // Tokenise the line
        final String[] newLine = IRCParser.tokeniseLine(line);

        if (!checkParamCount(newLine, 1)) {
            return;
        }

        newLine[0] = newLine[0].toUpperCase();

        // Handle a few requests here where being authenticated or not doesn't
        // matter
        switch (newLine[0]) {
            case "QUIT":
                close("Client Quit: " + (newLine.length > 1 ? newLine[newLine.length - 1] : "No reason given."));
                return;
            case "PING":
                if (newLine.length > 1) {
                    sendLine(":%s PONG %1$s :%s", getServerName(), newLine[1]);
                } else {
                    sendLine(":%s PONG %1$s :%s", getServerName(), System.currentTimeMillis());
                }
                return;
            case "PONG":
                return;
            case "NOTICE":
                // Is this a CTCP Reply to the bot? (used for versioning)
                if (newLine.length > 2 && newLine[1].equalsIgnoreCase(Util.getBotName()) && newLine[2].charAt(0) == (char)1 && newLine[2].charAt(newLine[2].length() - 1) == (char)1) {
                    final String[] version = newLine[2].split(" ", 2);
                    if (version.length > 1) {
                        clientVersion = version[1].substring(0, version[1].length() - 1);
                        return;
                    }
                }
                break;
            case "CAP":
                if (!checkParamCount(newLine, 2)) { return; }

                newLine[1] = newLine[1].toUpperCase();

                if (!isNegotiating && myAccount == null) {
                    isNegotiating = true;
                    negotiationLines.clear();
                }

                // LS shows all capabilities
                // LIST shows all enabled capabilities
                // CLEAR disables and shows all capabilities
                switch (newLine[1]) {
                    case "LS":
                    case "LIST":
                    case "CLEAR":
                        final boolean onlyEnabled = newLine[1].equals("LIST") || newLine[1].equals("CLEAR");
                        final boolean clearing = newLine[1].equals("CLEAR");

                        // Respond with our capabilities, or the enabled capabilities
                        // as requested.
                        final String prefix = String.format(":%s CAP %s %s ", getServerName(), (nickname == null) ? '*' : nickname, newLine[1]);

                        final StringBuilder caps = new StringBuilder();
                        for (final String cap : capabilities.keySet()) {
                            if (cap.contains(" ")) { continue; } // Spaces are invalid in capability names, used for internal capabilities.
                            if (onlyEnabled && getCapabilityState(cap) != CapabilityState.ENABLED) {
                                continue;
                            }
                            if (clearing) { setCapabilityState(cap, CapabilityState.DISABLED); }

                            // 500 is a safe limit for the line length, allowing for
                            // extra characters.
                            if (prefix.length() + caps.length() > 500) {
                                sendLine(prefix + "* :" + caps.toString().trim());
                                caps.setLength(0);
                            }

                            caps.append(" ");
                            if (clearing) { caps.append(CapabilityState.DISABLED.getModifier()); }
                            caps.append(cap);
                        }

                        sendLine(prefix + ":" + caps.toString().trim());
                        return;
                    case "REQ":
                        // Client requests capablities
                        final Map<String, CapabilityState> goodCaps = new HashMap<>();
                        final String[] reqCaps = newLine[newLine.length - 1].toLowerCase().split(" ");
                        for (String capability : reqCaps) {
                            if (capability.length() == 0) { continue; }
                            final String cap;
                            // Check for modifier
                            char modifier = capability.charAt(0);
                            // Check modifier is valid
                            if (CapabilityState.fromModifier(modifier) != CapabilityState.INVALID) {
                                if (capability.length() == 1) { continue; }
                                cap = capability.substring(1);
                            } else {
                                modifier = '+';
                                cap = capability;
                            }

                            // We have to accept the capabilities wholesale, or not at
                            // all (stupid), so check to see if we can accept this one
                            // and store it for a second round of processing...
                            if (capabilities.containsKey(cap)) {
                                goodCaps.put(cap, CapabilityState.fromModifier(modifier));
                            } else {
                                // Reject the lot, stupid standard.
                                sendLine(":%s CAP %s NAK :%s", getServerName(), (nickname == null) ? '*' : nickname, newLine[newLine.length - 1]);

                                sendLine(":%s CAP_DEBUG %s NAK :%s (%s)", getServerName(), (nickname == null) ? '*' : nickname, cap, modifier);
                                return;
                            }
                        }

                        // Ok, if we are here, check what CAPs were requested and do as
                        // requested.
                        for (Entry<String, CapabilityState> e : goodCaps.entrySet()) {
                            setCapabilityState(e.getKey(), e.getValue());

                            if (e.getKey().equals("dfbnc.com/tsirc")) {
                                // Send the TSIRC timestamp.
                                sendLine(":%s TSIRC %s %s :%s", getServerName(), "1", (System.currentTimeMillis()), "Timestamped IRC Enabled");
                            }
                        }

                        // Acknowledge the caps.
                        sendLine(":%s CAP %s ACK :%s", getServerName(), (nickname == null) ? '*' : nickname, newLine[newLine.length - 1]);
                        return;
                    case "ACK":
                        // Client acknowledges capabilities
                        // None of our capabilities require acking currently,
                        // so this is a noop.
                        return;
                    case "END":
                        // Client is done with the negotiation.
                        if (isNegotiating) {
                            isNegotiating = false;
                            for (final String negLine : negotiationLines) {
                                processLine(negLine);
                            }
                            negotiationLines.clear();
                        }
                        return;
                    default:
                        sendIRCLine(Consts.ERR_BADCAP, newLine[1], "Invalid CAP subcommand");
                        return;
                }
        }

        if (isNegotiating) {
            // This will only be true if we are pre-authentication, and is used
            // to suspend authentication.
            // If we are in the middle of negotiating capabilities, and this
            // message isn't related to a capability, store it for now and we
            // will replay it after negotiation has completed.
            negotiationLines.add(line);
            return;
        }

        // Pass it on the appropriate processing function
        if (myAccount != null) {
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
        if (!checkParamCount(line, 2)) {
            return;
        }

        switch (line[0]) {
            case "USER":
                // Username may be given in PASS so check that it hasn't before assigning
                if (username == null) { username = line[1]; }
                realname = line[line.length-1];
                if (nickname != null && password == null) {
                    sendBotMessage("Please enter your password.");
                    sendBotMessage("This can be done using either: ");
                    sendBotMessage("    /QUOTE PASS [<username>:]<password>");
                    sendBotMessage("    /RAW PASS [<username>:]<password>");
                }
                break;
            case "NICK":
                nickname = line[1];
                if (realname != null && password == null) {
                    sendBotMessage("Please enter your password.");
                    sendBotMessage("This can be done using either: ");
                    sendBotMessage("    /QUOTE PASS [<username>:]<password");
                    sendBotMessage("    /RAW PASS [<username>:]<password>");
                }
                sendBotLine("PRIVMSG", (char)1 + "VERSION" + (char)1);
                break;
            case "PASS":
                final String[] bits = line[line.length-1].split(":",2);
                if (bits.length == 2) {
                    username = bits[0];
                    password = bits[1];
                } else {
                    password = bits[0];
                }
                break;
            case "TIMESTAMPEDIRC":
            case "TSIRC":
                setCapabilityState("dfbnc.com/tsirc", CapabilityState.ENABLED);
                break;
            default:
                sendIRCLine(Consts.ERR_NOTREGISTERED, line[0], "You must login first.");
        }

        // After every message, check if we have everything we need...
        if (realname != null && password != null && nickname != null) {
            final String[] bits = username.split("\\+");
            username = bits[0];
            clientID = (bits.length > 1) ? bits[1].toLowerCase() : null;
            if (AccountManager.count() == 0 || (DFBnc.getBNC().allowAutoCreate() && !AccountManager.exists(username))) {
                Account acc = AccountManager.createAccount(username, password);
                    if (AccountManager.count() == 1) {
                        acc.setAdmin(true);
                        sendBotMessage("You are the first user of this bnc, and have been made admin");
                    } else {
                        sendBotMessage("The given account does not exist, so an account has been created for you.");
                    }
                    AccountManager.saveAccounts();
                    DFBnc.getBNC().getConfig().save();
            }
            if (AccountManager.checkPassword(username, clientID, password)) {
                myAccount = AccountManager.get(username);
                if (myAccount.isSuspended()) {
                    sendBotMessage("This account has been suspended.");
                    sendBotMessage("Reason: "+myAccount.getSuspendReason());
                    myAccount = null;
                    close("Account suspended.");
                } else {
                    sendBotMessage("You are now logged in");
                    if (myAccount.isAdmin()) {
                        sendBotMessage("This is an Admin account");
                    }
                    // Run the firsttime command if this is the first time the account has been used
                    if (myAccount.isFirst()) {
                        handleBotCommand(new String[]{"show", "firsttime"});
                        if (myAccount.isAdmin()) {
                            sendBotMessage("");
                            handleBotCommand(new String[]{"show", "firsttime", "admin"});
                        }
                    }
                    Logger.debug2("processNonAuthenticated - User Connected");
                    myAccount.userConnected(this);
                    Logger.debug2("userConnected finished");
                }
            } else {
                passwordTries++;
                final StringBuffer message = new StringBuffer("Password incorrect, or account not found.");
                message.append(" You have ");
                message.append(maxPasswordTries - passwordTries);
                message.append(" attempt(s) left.");
                sendIRCLine(Consts.ERR_PASSWDMISMATCH, line[0], message.toString());
                sendBotMessage(message.toString());
                password = null;
                if (passwordTries >= maxPasswordTries) {
                    sendIRCLine(Consts.ERR_PASSWDMISMATCH, line[0], "Too many password attempts, closing socket.");
                    sendBotMessage("Too many password attempts, closing socket.");
                    close("Too many password attempts.");
                }
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
            sendLine(":%s %03d %s :%s", getServerName(), numeric, params, line);
        } else {
            sendLine(":%s %03d %s %s", getServerName(), numeric, params, line);
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
        switch (line[0]) {
            case "PRIVMSG":
            case "NOTICE":
                if (line.length > 2) {
                    if (line[1].equalsIgnoreCase(Util.getBotName())) {
                        handleBotCommand(line[2].split(" "));
                        return;
                    } else {
                        final String myHost = (this.getAccount().getConnectionHandler() != null) ? this.getAccount().getConnectionHandler().getMyHost() : this.getNickname()+"!user@host" ;
                        if (myHost != null) {
                            sendAllChannel(line[1], String.format(":%s %s", myHost, normalLine), true);
                        }
                    }
                }
                break;
            case "DFBNC":
                String[] bits;
                if (line.length > 1) {
                    String[] lineBits = normalLine.split(" ");
                    bits = new String[lineBits.length-1];
                    System.arraycopy(lineBits, 1, bits, 0, lineBits.length-1);
                } else {
                    bits = new String[0];
                }
                handleBotCommand(bits);
                return;
            case "WHOIS":
                if (line[1].equalsIgnoreCase(Util.getBotName())) {
                    sendIRCLine(Consts.RPL_WHOISUSER, nickname+" "+Util.getBotName()+" bot "+getServerName()+" *", "DFBnc Pseudo Client");
                    sendIRCLine(Consts.RPL_WHOISSERVER, nickname+" "+Util.getBotName()+" DFBNC.Server", "DFBnc Pseudo Server");
                    sendIRCLine(Consts.RPL_WHOISIDLE, nickname+" "+Util.getBotName()+" 0 "+(DFBnc.getStartTime()/1000), "seconds idle, signon time");
                    sendIRCLine(Consts.RPL_ENDOFWHOIS, nickname+" "+Util.getBotName(), "End of /WHOIS list");
                    return;
                }
                break;
            case "TIMESTAMPEDIRC":
            case "TSIRC":
                if (line.length < 2 && line[1].equalsIgnoreCase("OFF")) {
                    setCapabilityState("dfbnc.com/tsirc", CapabilityState.DISABLED);
                    sendLine(":%s TSIRC %s %s :%s", getServerName(), "0", (System.currentTimeMillis()), "Timestamped IRC Disabled");
                } else if (line.length < 2 || line[1].equalsIgnoreCase("ON")) {
                    setCapabilityState("dfbnc.com/tsirc", CapabilityState.ENABLED);
                    sendLine(":%s TSIRC %s %s :%s", getServerName(), "1", (System.currentTimeMillis()), "Timestamped IRC Enabled");
                }
                return;
        }

        // We didn't handle this ourselves, send it to the ConnectionHandler
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
            if (DFBnc.getBNC().getConfig().getOptionBool("general", "allowshortcommands") && bits.length > 0) {
                final SortedMap<String, Command> cmds = new TreeMap<>(myAccount.getCommandManager().getAllCommands(bits[0], myAccount.isAdmin()));
                if (cmds.size() > 0) {
                    if (cmds.size() == 1) {
                        final String req = (bits.length > 0 ? bits[0] : "");
                        final String match = cmds.firstKey();
                        sendBotMessage("The command '%s' only matched a single command (%s). To prevent accidental use however, the full command is required.", req, match);
                        return;
                    } else {
                        sendBotMessage("Unknown command '%s' Please try 'show commands'", (bits.length > 0 ? bits[0] : ""));
                        sendBotMessage("Possible matching commands:");
                        sendBotMessage("----------");
                        for (Entry<String, Command> entry : cmds.entrySet()) {
                            if (entry.getKey().charAt(0) == '*') { continue; }
                            final Command command = entry.getValue();
                            if (!command.isAdminOnly() || myAccount.isAdmin()) {
                                sendBotMessage(String.format("%-20s - %s", entry.getKey(), command.getDescription(entry.getKey())));
                            }
                        }
                        return;
                    }
                }
            }
            sendBotMessage("Unknown command '%s' Please try 'show commands'", (bits.length > 0 ? bits[0] : ""));
        } catch (Exception e) {
            sendBotMessage("Exception with command '%s': %s", (bits.length > 0 ? bits[0] : ""), e.getMessage());
            e.printStackTrace();
        }
    }
}

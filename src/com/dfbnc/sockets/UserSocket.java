/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

import com.dfbnc.Account;
import com.dfbnc.ConnectionHandler;
import com.dfbnc.Consts;
import com.dfbnc.DFBnc;
import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandException;
import com.dfbnc.commands.CommandNotFoundException;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.commands.filters.CommandOutputFilter;
import com.dfbnc.commands.filters.CommandOutputFilterException;
import com.dfbnc.commands.filters.CommandOutputFilterManager;
import com.dfbnc.config.Config;
import com.dfbnc.sockets.secure.HandshakeCompletedEvent;
import com.dfbnc.sockets.secure.SSLByteChannel;
import com.dfbnc.util.MultiWriter;
import com.dfbnc.util.UserSocketMessageWriter;
import com.dfbnc.util.Util;
import com.dmdirc.parser.irc.CapabilityState;
import com.dmdirc.parser.irc.IRCParser;
import uk.org.dataforce.libs.logger.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.net.ssl.SSLPeerUnverifiedException;

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
    /** Given client SSL Cert Fingerprint */
    private String clientCertFP = null;
    /** Client Type (used for workarounds) */
    private ClientType clientType = ClientType.getFromVersion("");
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

    /** IP Address of this socket */
    private String myIP = "0.0.0.0";

    /** The Account object for this connect (This is null before authentication) */
    private Account myAccount = null;

    /** Is closeAll being run? (This prevents socketClosed removing the HashMap entry) */
    private static boolean closeAll = false;

    /** Is this socket in the middle of capability negotiation? */
    private boolean isNegotiating = false;

    /** Is this socket in the middle of quitting? */
    private boolean isQuitting = false;

    /** Lines buffered during negotiation. */
    private List<String> negotiationLines = new LinkedList<>();

    /** Map of capabilities and their state. */
    private final Map<String, CapabilityState> capabilities = new HashMap<>();

    /** Capabilities that enable tags. */
    private final Set<String> tagCapabilities = new HashSet<>();

    /** Are message tags allowed? */
    private boolean allowTags = false;

    /** Map of objects associated with this UserSocket. */
    private final static HashMap<Object, Object> myMap = new HashMap<>();

    /** Set of debug flags. */
    private final Set<DebugFlag> debugFlags = new HashSet<>();

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

            // TODO: Handle this better.
            tagCapabilities.add("server-time");
            tagCapabilities.add("batch");
            tagCapabilities.add("dfbnc.com/channelhistory");
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
     * Get the client type for this socket.
     *
     * @return The client Version for this socket.
     */
    public ClientType getClientType() {
        return clientType;
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
                if (state == CapabilityState.ENABLED && tagCapabilities.contains(capability.toLowerCase())) {
                    allowTags = true;
                }
            }
        }
    }

    /**
     * Does this socket support message tags?
     *
     * @return True if this socket has ever enabled a capability that is
     *         delivered via message tags.
     */
    public boolean allowTags() {
        return allowTags;
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
     * @return a Collection of all UserSockets that are part of the given account
     */
    public static List<UserSocket> getUserSockets() {
        return new ArrayList<>(knownSockets.values());
    }

    /**
     * Get a List of all UserSockets that are part of a given account
     *
     * @param account Account to check sockets against
     * @return a Collection of all UserSockets that are part of the given account
     */
    public static List<UserSocket> getUserSockets(final Account account) {
        final ArrayList<UserSocket> list = new ArrayList<>();
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

    @Override
    public void socketOpened() {
        sendBotMessage("Welcome to DFBnc (%s)", DFBnc.getVersion());
        if (isSSL) {
            sendBotMessage("You are connected using SSL");
        } else {
            sendBotMessage("You are not connected using SSL");
        }
    }

    @Override
    public void handshakeCompleted(final HandshakeCompletedEvent hce) {
        try {
            final String fingerprint = Util.sha1(hce.getPeerCertificates()[0].getEncoded()).toUpperCase();
            sendBotMessage("Your SSL Client Certificate Fingerprint is: " + fingerprint);
            clientCertFP = fingerprint;
        } catch (final SSLPeerUnverifiedException | CertificateEncodingException ex) { /* Don't Care. */ }
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
     * Get the global config for the account linked to this socket
     *
     * @return Config object for the account that is associated with this socket
     */
    public Config getAccountConfig() {
        return getAccount().getAccountConfig();
    }

    /**
     * Get the subclient config for the account linked to this socket
     *
     * @return subclient Config object for the account that is associated with this socket
     */
    public Config getClientConfig() {
        return getAccount().getConfig(getClientID());
    }

    /**
     * Is this socket considered an active client?
     *
     * @return True if this socket is considered an active client.
     */
    public boolean isActiveClient() {
        return getClientConfig().getOptionBool("user", "activeclient");
    }

    /**
     * Is this socket a read-only client?
     *
     * @return True if this socket is considered a read-only.
     */
    public boolean isReadOnly() {
        return getClientID() != null && getClientConfig().getOptionBool("user", "readonly");
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
        final String outLine = args.length == 0 ? data : String.format(data, args);

        if (post001) {
            if (myAccount != null) {
                final String method = myAccount.getContactMethod();
                if (method.equalsIgnoreCase("SNOTICE")) {
                    sendServerLine("NOTICE", data, args);
                } else if (method.equalsIgnoreCase("AUTH")) {
                    sendLine("NOTICE AUTH :- %s", outLine);
                } else {
                    sendBotLine(method, data, args);
                }
            } else {
                sendServerLine("NOTICE", data, args);
            }
        } else {
            sendLine("NOTICE AUTH :- %s", outLine);
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
        final String outLine = args.length == 0 ? data : String.format(data, args);
        sendLine(":%s!bot@%s %s %s :%s", Util.getBotName(), getServerName(), type, target, outLine);
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
            boolean allowedChannel = (myAccount.getConnectionHandler() == null) || myAccount.getConnectionHandler().activeAllowedChannel(this, channel);
            if (ignoreThis && socket == this || !allowedChannel) {
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
        final String outLine = args.length == 0 ? data : String.format(data, args);
        sendLine(":%s!bot@%s %s %s :%s", Util.getBotName(), getServerName(), type, nickname, outLine);
    }

    /**
     * Send a message to the user from the bnc debug bot in printf format.
     *
     * @param type the Type of message to send
     * @param data The format string
     * @param args The args for the format string
     */
    public void sendDebugBotLine(final String type, final String data, final Object... args) {
        final String outLine = args.length == 0 ? data : String.format(data, args);
        sendLine(":%s!bot@%s %s %s :%s", Util.getBotName() + "_DEBUG", getServerName(), type, nickname, outLine);
    }

    /**
     * Send a message to the user from the bnc server in printf format.
     *
     * @param type the Type of message to send
     * @param data The format string
     * @param args The args for the format string
     */
    public void sendServerLine(final String type, final String data, final Object... args) {
        final String outLine = args.length == 0 ? data : String.format(data, args);
        sendLine(":%s %s %s :%s", getServerName(), type, nickname, outLine);
    }

    @Override
    protected void socketClosed(final boolean userRequested) {
        if (!closeAll) {
            synchronized (knownSockets) {
                knownSockets.remove(myID);
            }
        }

        Logger.info("User Disconnected: " + myInfo);
        getDebugFlags().stream().forEach(df -> setDebugFlag(df, false));

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

    @Override
    protected void processLine(final String line) {
        // Reset the inactive counter.
        this.inactiveCounter = 0;

        // Don't process any more lines if we are quitting.
        if (isQuitting) { return; }

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
                isQuitting = true;
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
                if (newLine.length > 2 && newLine[1].toLowerCase().startsWith(Util.getBotName().toLowerCase()) && newLine[2].charAt(0) == (char)1 && newLine[2].charAt(newLine[2].length() - 1) == (char)1) {
                    final String[] version = newLine[2].split(" ", 2);
                    if (version.length > 1) {
                        clientVersion = version[1].substring(0, version[1].length() - 1);
                        // If we haven't worked out the client type elsewhere
                        // (eg from the username) then try to parse the version.
                        if (clientType == ClientType.Generic) {
                            clientType = ClientType.getFromVersion(clientVersion);
                        }
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
            clientID = (bits.length > 1 && !bits[1].isEmpty()) ? bits[1].replaceAll("[^a-z0-9_-]", "") : null;
            if (bits.length > 2 && !bits[2].isEmpty()) {
                clientType = ClientType.getFromName(bits[2].toLowerCase());
            }
            if (DFBnc.getAccountManager().count() == 0 || (DFBnc.getBNC().allowAutoCreate() && !DFBnc.getAccountManager().exists(username))) {
                Account acc = DFBnc.getAccountManager().createAccount(username, password);
                    if (DFBnc.getAccountManager().count() == 1) {
                        acc.setAdmin(true);
                        sendBotMessage("You are the first user of this bnc, and have been made admin");
                    } else {
                        sendBotMessage("The given account does not exist, so an account has been created for you.");
                    }
                DFBnc.getAccountManager().saveAccounts();
                    DFBnc.getBNC().getConfig().save();
            }

            if (DFBnc.getAccountManager().checkPassword(username, clientID, password)) {
                myAccount = DFBnc.getAccountManager().get(username);
                if (myAccount.isSuspended()) {
                    sendBotMessage("This account has been suspended.");
                    sendBotMessage("Reason: %s", myAccount.getSuspendReason());
                    myAccount = null;
                    close("Account suspended.");
                } else {
                    sendBotMessage("You are now logged in");
                    if (myAccount.isAdmin()) {
                        sendBotMessage("This is an Admin account");
                    }
                    // Run the firsttime command if this is the first time the account has been used
                    if (myAccount.isFirst()) {
                        final CommandOutputBuffer co = new CommandOutputBuffer(this);
                        handleBotCommand(new String[]{"show", "firsttime"}, co);
                        if (myAccount.isAdmin()) {
                            sendBotMessage("");
                            handleBotCommand(new String[]{"show", "firsttime", "admin"}, co);
                        }
                        co.send();
                    }
                    Logger.debug2("processNonAuthenticated - User Connected");
                    myAccount.userConnected(this);
                    Logger.debug2("userConnected finished");
                }
            } else {
                passwordTries++;
                final StringBuilder message = new StringBuilder("Password incorrect, or account not found.");
                message.append(" You have ");
                // TODO: make this a config setting
                int maxPasswordTries = 3;
                message.append(maxPasswordTries - passwordTries);
                message.append(" attempt(s) left.");
                sendIRCLine(Consts.ERR_PASSWDMISMATCH, line[0], message.toString());
                sendBotMessage("%s", message.toString());

                // TODO: Remove this warning at some poing as it gives too much away.
                if (DFBnc.getAccountManager().checkPassword(username, clientID, password.toLowerCase())) {
                    sendBotMessage("%s", "WARNING: Your password was previously hashed non case-sensitively.");
                    sendBotMessage("%s", "WARNING: Please try connecting with a lowercase password and then changing it.");
                }

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

        // We might change what we want to pass to the Connection Handler
        // (eg in the case of tapchat workarounds). If we do, this will be
        // non-null.
        String alternativeSendLine = null;

        // The bnc accepts commands as either:
        // /msg -BNC This is a command
        // or /DFBNC This is a command (note there is no : used to separate arguments anywhere)
        switch (line[0]) {
            case "PRIVMSG":
            case "NOTICE":
                if (line.length > 2) {
                    if (line[1].toLowerCase().startsWith(Util.getBotName().toLowerCase())) {
                        final CommandOutputBuffer co = new CommandOutputBuffer(this);
                        handleBotCommand(line[2].split(" "), co);
                        co.send();
                        return;
                    } else {
                        if (getClientType() == ClientType.TapChat && line.length > 2 && (line[2].startsWith("./"))) {
                            // Command from tapchat, we should handle it here.
                            final String[] bits = line[2].split(" ", 2);
                            if (bits[0].equalsIgnoreCase("./me")) {
                                final char char1 = (char) 1;
                                alternativeSendLine = (bits.length > 1) ? String.format("%s %s :%sACTION %s%s", line[0], line[1], char1, bits[1], char1) : "";
                            } else if (bits[0].equalsIgnoreCase("./nick")) {
                                alternativeSendLine = (bits.length > 1) ? "NICK :" + bits[1] : "";
                                break;
                            } else if (bits[0].equalsIgnoreCase("./away")) {
                                alternativeSendLine = (bits.length > 1) ? "AWAY :" + bits[1] : "AWAY";
                                break;
                            }
                        }

                        final String myHost = (this.getAccount().getConnectionHandler() != null) ? this.getAccount().getConnectionHandler().getMyHost() : this.getNickname()+"!user@host" ;
                        if (myHost != null) {
                            if (alternativeSendLine == null) {
                                sendAllChannel(line[1], String.format(":%s %s", myHost, normalLine), true);
                            } else {
                                sendAllChannel(line[1], String.format(":%s %s", myHost, alternativeSendLine), true);
                            }
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
                final CommandOutputBuffer co = new CommandOutputBuffer(this);
                handleBotCommand(bits, co);
                co.send();
                return;
            case "WHOIS":
                if (line[1].toLowerCase().startsWith(Util.getBotName().toLowerCase())) {
                    sendIRCLine(Consts.RPL_WHOISUSER, nickname+" "+line[1]+" bot "+getServerName()+" *", "DFBnc Pseudo Client");
                    sendIRCLine(Consts.RPL_WHOISSERVER, nickname+" "+line[1]+" DFBNC.Server", "DFBnc Pseudo Server");
                    sendIRCLine(Consts.RPL_WHOISIDLE, nickname+" "+line[1]+" 0 "+(DFBnc.getStartTime()/1000), "seconds idle, signon time");
                    sendIRCLine(Consts.RPL_ENDOFWHOIS, nickname+" "+line[1], "End of /WHOIS list");
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
            if (alternativeSendLine == null) {
                myConnectionHandler.dataReceived(this, normalLine, line);
            } else if (!alternativeSendLine.isEmpty()) {
                myConnectionHandler.dataReceived(this, alternativeSendLine, IRCParser.tokeniseLine(alternativeSendLine));
            }
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
    private void handleBotCommand(final String[] bits, final CommandOutputBuffer output) {
        final List<String[]> sections = new LinkedList<>();

        // TODO: Filtering doesn't make sense in all cases, and should probably
        //       be limited to a set of filter-able commands, so that escaping
        //       of "|" is not needed in all cases.

        List<String> thisSection = new LinkedList<>();
        boolean nextCommand = false;
        for (String b : bits) {
            if (b.length() > 0 && b.charAt(0) == '|') {
                b = b.substring(1);
                // Look for escaped |
                if (b.isEmpty() || b.charAt(0) != '|') {
                    nextCommand = true;
                    if (b.isEmpty()) { continue; }
                }
            }

            if (nextCommand) {
                sections.add(thisSection.toArray(new String[thisSection.size()]));
                thisSection.clear();
                nextCommand = false;
            }

            thisSection.add(b);
        }
        if (!thisSection.isEmpty()) {
            sections.add(thisSection.toArray(new String[thisSection.size()]));
        }

        // for (String[] s : sections) { System.out.println(Arrays.toString(s)); }

        // Run the actual command.
        if (doBotCommand(sections.get(0), output)) {
            // Store current messages.
            final List<String> oldMessages = output.getMessages();

            // Now run any filters. If any of them fail or throw exceptions,
            // we will restore the commandoutput back to unfiltered and not
            // run any more.
            String[] section = new String[0];
            try {
                if (sections.size() > 1) {
                    for (int i = 1; i < sections.size(); i++) {
                        section = sections.get(i);
                        final CommandOutputFilter cof = CommandOutputFilterManager.getFilter(section[0]);
                        final String[] filterParams = section.length > 1 ? Arrays.copyOfRange(section, 1, section.length) : new String[0];

                        if (cof != null) {
                            cof.runFilter(filterParams, output);
                        } else {
                            throw new CommandOutputFilterException("Unknown filter.");
                        }
                    }
                }
            } catch (final CommandOutputFilterException ex) {
                output.setMessages(oldMessages);
                output.addBotMessage("--------------------------------------");
                output.addBotMessage("Error with filter: %s", Arrays.toString(section));
                output.addBotMessage("Reason: %s", ex.getMessage());
            }
        }
    }


    /**
     * Actually Handle the command that was sent to the bot.
     *
     * @param bits This is the command and its parameters.
     *             bits[0] is the command, bits[1]..bits[n] are the params.
     * @return True is a command actually ran, else false.
     */
    private boolean doBotCommand(final String[] bits, final CommandOutputBuffer output) {
        try {
            if (myAccount != null) {
                myAccount.getCommandManager().handle(this, bits, output);
                return true;
            }
        } catch (CommandNotFoundException c) {
            if (DFBnc.getBNC().getConfig().getOptionBool("general", "allowshortcommands") && bits.length > 0) {
                final SortedMap<String, Command> cmds = new TreeMap<>(myAccount.getCommandManager().getAllCommands(bits[0], (myAccount.isAdmin() && !isReadOnly())));
                if (cmds.size() > 0) {
                    if (cmds.size() == 1) {
                        final String req = (bits.length > 0 ? bits[0] : "");
                        final String match = cmds.firstKey();
                        output.addBotMessage("The command '%s' only matched a single command (%s). To prevent accidental use however, the full command is required.", req, match);
                        return false;
                    } else {
                        output.addBotMessage("Unknown command '%s' Please try 'show commands'", (bits.length > 0 ? bits[0] : ""));
                        output.addBotMessage("Possible matching commands:");
                        output.addBotMessage("----------");
                        for (Entry<String, Command> entry : cmds.entrySet()) {
                            if (entry.getKey().charAt(0) == '*') { continue; }
                            final Command command = entry.getValue();
                            if (!command.isAdminOnly() || (myAccount.isAdmin() && !isReadOnly())) {
                                output.addBotMessage("%-20s - %s", entry.getKey(), command.getDescription(entry.getKey()));
                            }
                        }
                        return false;
                    }
                }
            }
            output.addBotMessage("Unknown command '%s' Please try 'show commands'", (bits.length > 0 ? bits[0] : ""));
        } catch (CommandException e) {
            output.addBotMessage("Exception with command '%s': %s", (bits.length > 0 ? bits[0] : ""), e.getMessage());
            e.printStackTrace();
            return false;
        }

        return false;
    }

    private void handleDebugFlagLogging(final boolean value) {
        final UserSocketMessageWriter usmw;
        if (getMap().containsKey("usmw_loggingdebug")) {
            usmw = (UserSocketMessageWriter)getMap().get("usmw_loggingdebug");
        } else {
            usmw = new UserSocketMessageWriter(this, DebugFlag.Logging.getTag());
            getMap().put("usmw_loggingdebug", usmw);
        }

        final MultiWriter multiWriter = DFBnc.getBNC().getMultiWriter();
        if (value) {
            multiWriter.addWriter(usmw);
        } else {
            multiWriter.removeWriter(usmw);
            getMap().remove("usmw_loggingdebug");
        }
    }

    /**
     * Set the given debug flag value.
     *
     * @param flag DebugFlag to change
     * @param newValue New value to set.
     * @return True if flag was enabled, else false.
     */
    public boolean setDebugFlag(final DebugFlag flag, final boolean newValue) {
        if (newValue) {
            debugFlags.add(flag);

            // Enable the flag in the connection handler.
            if (myAccount != null && myAccount.getConnectionHandler() != null) {
                myAccount.getConnectionHandler().enableDebug(flag);
            }

            if (flag == DebugFlag.Logging) { handleDebugFlagLogging(true); }
        } else {
            debugFlags.remove(flag);

            // Disable the flag in the connection handler if none of
            // the other sockets that share his account still have it enabled.
            if (myAccount != null) {
                final long flagCount = myAccount.getUserSockets().stream().filter(u -> u.debugFlagEnabled(flag)).count();

                if (flagCount == 0 && myAccount.getConnectionHandler() != null) {
                    myAccount.getConnectionHandler().disableDebug(flag);
                }

                if (flag == DebugFlag.Logging) { handleDebugFlagLogging(false); }
            }
        }

        return newValue;
    }

    /**
     * Check if the given debug flag is enabled.
     *
     * @param flag DebugFlag to check
     * @return True if flag was enabled, else false.
     */
    public boolean debugFlagEnabled(final DebugFlag flag) {
        return debugFlags.contains(flag);
    }

    /**
     * Get a list of enabled debug flags
     *
     * @return List of enabled debug flags
     */
    public List<DebugFlag> getDebugFlags() {
        return Arrays.asList(debugFlags.toArray(new DebugFlag[0]));
    }
}

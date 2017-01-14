/*
 * Copyright (c) 2006-2017 DFBnc Developers
 * Copyright (c) 2006-2015 DMDirc Developers
 *
 * Where no other license is explicitly given or mentioned in the file, all files
 * in this project are licensed using the following license.
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

package com.dfbnc.servers.logging;

import com.dfbnc.Account;
import com.dfbnc.ConnectionHandler;
import com.dmdirc.util.io.StreamUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import net.engio.mbassy.listener.Handler;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import com.dmdirc.parser.events.PrivateActionEvent;
import com.dmdirc.parser.events.PrivateMessageEvent;
import com.dmdirc.parser.events.ChannelMessageEvent;
import com.dmdirc.parser.events.ChannelActionEvent;
import com.dmdirc.parser.events.ChannelTopicEvent;
import com.dmdirc.parser.events.ChannelJoinEvent;
import com.dmdirc.parser.events.ChannelSelfJoinEvent;
import com.dmdirc.parser.events.ChannelPartEvent;
import com.dmdirc.parser.events.ChannelQuitEvent;
import com.dmdirc.parser.events.ChannelKickEvent;
import com.dmdirc.parser.events.ChannelNickChangeEvent;
import com.dmdirc.parser.events.ChannelModeChangeEvent;
import com.dmdirc.parser.events.ChannelNoticeEvent;
import com.dmdirc.parser.events.PrivateNoticeEvent;

import com.dmdirc.parser.events.SocketCloseEvent;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Missing.
// import com.dmdirc.parser.events.ChannelModeMessageEvent;
// import com.dmdirc.parser.events.ChannelModeNoticeEvent;
// import com.dmdirc.parser.events.ChannelCTCPEvent;
// import com.dmdirc.parser.events.ChannelCTCPReplyEvent;

/**
 * Basic Server Logger handling DMDIRC Parser Events.
 * This class is based on parts of the DMDirc Logging Plugin.
 */
public class ServerLogger {

    /** Date format used for "File Opened At" log. */
    private static final DateFormat OPENED_AT_FORMAT = new SimpleDateFormat("EEEE MMMM dd, yyyy - HH:mm:ss");
    /** Date format used for Normal log lines. */
    private static final DateFormat LOG_FORMAT = new SimpleDateFormat("[dd/MM/yyyy HH:mm:ss]");
    /** Map of open files. */
    private final Map<String, OpenFile> openFiles = Collections.synchronizedMap(new HashMap<>());
    /** The account we are logging for. */
    private final Account myAccount;
    /** The connection handler we are logging for. */
    private final ConnectionHandler myConnectionHandler;
    /** Timer used to close idle files. */
    private final Timer idleFileTimer;
    /** Log file Locator */
    protected final LogFileLocator locator;
    /** Do we want to add channel modes to log messages. */
    private final boolean channelmodeprefix = true;

    /** Have we been disabled? */
    private final AtomicBoolean disabled = new AtomicBoolean(false);

    /**
     * Parser Local Client.
     *
     * We keep a copy of this from our most recent self-join because
     * SocketClosed can happen after the state has reset, which is shit.
     */
    protected ClientInfo localClient = null;

    /**
     * List of channels we last knew we were in.
     *
     * We keep our own channel state for throwing channelQuits on socketclosed.
     */
    protected final List<ChannelInfo> myChannels = new LinkedList<>();

    /**
     * Create a ServerLogger
     *
     * @param account Account we are logging for.
     * @param connectionHandler ConnectionHandler we are logging.
     *
     * @throws Exception if we are unable to create the logs directory.
     */
    public ServerLogger(final Account account, final ConnectionHandler connectionHandler) throws Exception {
        myAccount = account;
        myConnectionHandler = connectionHandler;

        if (connectionHandler.getParser() == null) {
            throw new Exception("A parser is required before a ServerLogger can be created.");
        }

        locator = new LogFileLocator(myAccount);

        // Close idle files every hour.
        idleFileTimer = new Timer("Logging Timer [" + myAccount.getName() + "]");
        idleFileTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerTask();
            }
        }, 3600000);

        myConnectionHandler.subscribe(this);
    }

    public void disableLogging() {
        if (disabled.get()) { return; }

        handleSocketClose(new SocketCloseEvent(myConnectionHandler.getParser(), LocalDateTime.now()));
        disabled.set(true);

        if (idleFileTimer != null) {
            idleFileTimer.cancel();
            idleFileTimer.purge();
        }

        // Close all the open channels.
        for (final ChannelInfo c : new LinkedList<>(myChannels)) {
            final String filename = locator.getLogFile(c);
            if (filename == null) { continue; }

            appendLine(filename, "");
            appendLine(filename, "*** Channel closed at: %s", OPENED_AT_FORMAT.format(new Date()));
            myChannels.remove(c);
        }

        synchronized (openFiles) {
            openFiles.values().stream().forEach(file -> StreamUtils.close(file.writer));
            openFiles.clear();
        }

        myConnectionHandler.unsubscribe(this);
    }

    /**
     * What to do every hour when the timer fires.
     */
    protected void timerTask() {
        // Oldest time to allow
        final long oldestTime = System.currentTimeMillis() - 3480000;

        synchronized (openFiles) {
            final Collection<String> old = new ArrayList<>(openFiles.size());
            for (Map.Entry<String, OpenFile> entry : openFiles.entrySet()) {
                if (entry.getValue().lastUsedTime < oldestTime) {
                    StreamUtils.close(entry.getValue().writer);
                    old.add(entry.getKey());
                }
            }

            openFiles.keySet().removeAll(old);
        }
    }

    @Handler
    public void handleQueryActions(final PrivateActionEvent event) {
        final ClientInfo user = event.getParser().getClient(event.getHost());
        final String filename = locator.getLogFile(user);
        if (filename == null) { return; }
        appendLine(filename, "* %s %s", user.getNickname(), event.getMessage());
    }

    @Handler
    public void handleQueryMessages(final PrivateMessageEvent event) {
        final ClientInfo user = event.getParser().getClient(event.getHost());
        final String filename = locator.getLogFile(user);
        if (filename == null) { return; }
        appendLine(filename, "<%s> %s", user.getNickname(), event.getMessage());
    }

    @Handler
    public void handleQueryNotices(final PrivateNoticeEvent event) {
        final ClientInfo user = event.getParser().getClient(event.getHost());
        final String filename = locator.getLogFile(user);
        if (filename == null) { return; }
        appendLine(filename, "-%s- %s", user.getNickname(), event.getMessage());
    }

    @Handler
    public void handleChannelMessage(final ChannelMessageEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }
        appendLine(filename, "<%s> %s", getDisplayName(event.getClient()), event.getMessage());
    }

    @Handler
    public void handleChannelNotice(final ChannelNoticeEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }
        appendLine(filename, "-%s- %s", getDisplayName(event.getClient()), event.getMessage());
    }

    @Handler
    public void handleChannelAction(final ChannelActionEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }
        appendLine(filename, "* %s %s", getDisplayName(event.getClient()), event.getMessage());
    }

    @Handler
    public void handleChannelGotTopic(final ChannelTopicEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        if (event.isJoinTopic()) {

            if (!event.getChannel().getTopic().isEmpty()) {
                appendLine(filename, "*** Topic is: %s", event.getChannel().getTopic());
                appendLine(filename, "*** Set at: %s on %s by %s",
                        timeFormat.format(1000 * event.getChannel().getTopicTime()),
                        dateFormat.format(1000 * event.getChannel().getTopicTime()),
                        event.getChannel().getTopicSetter());
            }
        } else {
            appendLine(filename, "*** %s Changed the topic to: %s", event.getChannel().getTopicSetter(), event.getChannel().getTopic());
        }
    }

    @Handler
    public void handleChannelJoin(final ChannelJoinEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        final ChannelClientInfo channelClient = event.getClient();
        appendLine(filename, "*** %s (%s) joined the channel", getDisplayName(channelClient), getFullHostname(channelClient));
    }

    @Handler
    public void handleChannelSelfJoin(final ChannelSelfJoinEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        localClient = event.getParser().getLocalClient();
        synchronized (myChannels) {
            if (!myChannels.contains(event.getChannel())) {
                myChannels.add(event.getChannel());
                appendLine(filename, "*** Channel opened at: %s", OPENED_AT_FORMAT.format(new Date()));
                appendLine(filename, "");
            }
        }

        final ChannelClientInfo channelClient = event.getChannel().getChannelClient(event.getParser().getLocalClient());
        appendLine(filename, "*** %s (%s) joined the channel", getDisplayName(channelClient), getFullHostname(channelClient));
    }

    @Handler
    public void handleChannelPart(final ChannelPartEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        final ChannelClientInfo channelClient = event.getClient();

        final String message = event.getReason();

        if (message.isEmpty()) {
             appendLine(filename, "*** %s (%s) left the channel", getDisplayName(channelClient), getFullHostname(channelClient));
        } else {
            appendLine(filename, "*** %s (%s) left the channel (%s)", getDisplayName(channelClient), getFullHostname(channelClient), message);
        }

        if (channelClient.getClient() == localClient) {
            synchronized(myChannels) {
                if (myChannels.contains(event.getChannel())) {
                    appendLine(filename, "");
                    appendLine(filename, "*** Channel closed at: %s", OPENED_AT_FORMAT.format(new Date()));
                    myChannels.remove(event.getChannel());
                }
            }

            if (openFiles.containsKey(filename)) {
                StreamUtils.close(openFiles.get(filename).writer);
                openFiles.remove(filename);
            }
        }
    }

    @Handler
    public void handleSocketClose(final SocketCloseEvent event) {
        for (final ChannelInfo c : new LinkedList<>(myChannels)) {
            handleChannelQuit(new ChannelQuitEvent(event.getParser(), event.getDate(), c, c.getChannelClient(localClient), "Socket Closed"));
        }
    }

    @Handler
    public void handleChannelQuit(final ChannelQuitEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        final String reason = event.getReason();
        final ChannelClientInfo channelClient = event.getClient();

        if (reason.isEmpty()) {
            appendLine(filename, "*** %s (%s) Quit IRC", getDisplayName(channelClient), getFullHostname(channelClient));
        } else {
            appendLine(filename, "*** %s (%s) Quit IRC (%s)", getDisplayName(channelClient), getFullHostname(channelClient), reason);
        }

        if (channelClient.getClient() == localClient) {
            synchronized(myChannels) {
                if (myChannels.contains(event.getChannel())) {
                    appendLine(filename, "");
                    appendLine(filename, "*** Channel closed at: %s", OPENED_AT_FORMAT.format(new Date()));
                    myChannels.remove(event.getChannel());
                }
            }

            if (openFiles.containsKey(filename)) {
                StreamUtils.close(openFiles.get(filename).writer);
                openFiles.remove(filename);
            }
        }
    }

    @Handler
    public void handleChannelKick(final ChannelKickEvent event) {
        final ChannelClientInfo victim = event.getKickedClient();
        final ChannelClientInfo perpetrator = event.getClient();
        final String reason = event.getReason();
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        if (reason.isEmpty()) {
            appendLine(filename, "*** %s was kicked by %s", getDisplayName(victim), getDisplayName(perpetrator));
        } else {
            appendLine(filename, "*** %s was kicked by %s (%s)", getDisplayName(victim), getDisplayName(perpetrator), reason);
        }

        if (victim.getClient() == localClient) {
            appendLine(filename, "");
            appendLine(filename, "*** Channel closed at: %s", OPENED_AT_FORMAT.format(new Date()));
            synchronized(myChannels) {
                if (myChannels.contains(event.getChannel())) {
                    appendLine(filename, "");
                    appendLine(filename, "*** Channel closed at: %s", OPENED_AT_FORMAT.format(new Date()));
                    myChannels.remove(event.getChannel());
                }
            }

            if (openFiles.containsKey(filename)) {
                StreamUtils.close(openFiles.get(filename).writer);
                openFiles.remove(filename);
            }
        }
    }

    @Handler
    public void handleNickChange(final ChannelNickChangeEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        appendLine(filename, "*** %s is now %s", getDisplayName(event.getClient(),event.getOldNick()), getDisplayName(event.getClient()));
    }

    @Handler
    public void handleModeChange(final ChannelModeChangeEvent event) {
        final String filename = locator.getLogFile(event.getChannel());
        if (filename == null) { return; }

        if (event.getHost().isEmpty()) {
            appendLine(filename, "*** Channel modes are: %s", event.getModes());
        } else {
            appendLine(filename, "*** %s set modes: %s", getDisplayName(event.getClient()), event.getModes());
        }
    }

    /**
     * Add a line to a file.
     *
     * @param filename Name of file to write to
     * @param format   Format of line to add. (NewLine will be added Automatically)
     * @param args     Arguments for format
     *
     * @return true on success, else false.
     */
    protected boolean appendLine(final String filename, final String format, final Object... args) {
        return appendLine(filename, String.format(format, args));
    }

    /**
     * Add a line to a file.
     *
     * @param filename Name of file to write to
     * @param line     Line to add. (NewLine will be added Automatically)
     *
     * @return true on success, else false.
     */
    protected boolean appendLine(final String filename, final String line) {
        if (myAccount.getAccountConfig().getOptionBool("server", "logging") == false) { return true; }
        if (disabled.get()) { return false; }

        final StringBuilder finalLine = new StringBuilder();

        final String dateString = LOG_FORMAT.format(new Date()).trim();
        finalLine.append(dateString);
        finalLine.append(' ');

        finalLine.append(line);

        try {
            final BufferedWriter out;
            if (openFiles.containsKey(filename)) {
                final OpenFile of = openFiles.get(filename);
                of.lastUsedTime = System.currentTimeMillis();
                out = of.writer;
            } else {
                out = new BufferedWriter(new FileWriter(filename, true));
                openFiles.put(filename, new OpenFile(out));
            }
            out.write(finalLine.toString());
            out.newLine();
            out.flush();
            return true;
        } catch (IOException e) {
            /*
             * Do Nothing
             *
             * Makes no sense to keep adding errors to the logger when we can't write to the file,
             * as chances are it will happen on every incomming line.
             */
        }
        return false;
    }

    /**
     * Get full hostname name for channelClient.
     *
     * @param channelClient Get full hostname name for channelClient (nick|user@host)
     *
     * @return name to display
     */
    protected String getFullHostname(final ChannelClientInfo channelClient) {
        return channelClient.getClient().getNickname() + "!" + channelClient.getClient().getUsername() + "@" + channelClient.getClient().getHostname();
    }

    /**
     * Get name to display for channelClient (Taking into account the channelmodeprefix setting).
     *
     * @param channelClient The client to get the display name for
     *
     * @return name to display
     */
    protected String getDisplayName(final ChannelClientInfo channelClient) {
        return getDisplayName(channelClient, "");
    }

    /**
     * Get name to display for channelClient (Taking into account the channelmodeprefix setting).
     *
     * @param channelClient The client to get the display name for
     * @param overrideNick  Nickname to display instead of real nickname
     *
     * @return name to display
     */
    protected String getDisplayName(final ChannelClientInfo channelClient, final String overrideNick) {
        if (channelClient == null) {
            return overrideNick.isEmpty() ? "Unknown Client" : overrideNick;
        } else if (overrideNick.isEmpty()) {
            return channelmodeprefix ? channelClient.getImportantModePrefix() + channelClient.getClient().getNickname() : channelClient.getClient().getNickname();
        } else {
            return channelmodeprefix ? channelClient.getImportantMode() + overrideNick : overrideNick;
        }
    }


    /** Open File. */
    private static class OpenFile {

        /** Last used time. */
        public long lastUsedTime = System.currentTimeMillis();
        /** Open file's writer. */
        public final BufferedWriter writer;

        /**
         * Creates a new open file.
         *
         * @param writer Writer that has file open
         */
        protected OpenFile(final BufferedWriter writer) {
            this.writer = writer;
        }

    }
}

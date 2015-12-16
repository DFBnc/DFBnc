/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

package com.dfbnc.servers.irc;

import com.dfbnc.Account;
import com.dfbnc.servers.logging.ServerLogger;
import com.dmdirc.parser.events.ChannelMessageEvent;
import com.dmdirc.parser.events.PrivateMessageEvent;
import com.dmdirc.parser.events.ChannelNoticeEvent;
import com.dmdirc.parser.events.PrivateNoticeEvent;
import com.dmdirc.parser.events.ChannelActionEvent;
import com.dmdirc.parser.events.PrivateActionEvent;
// import com.dmdirc.parser.events.ChannelCTCPEvent;
// import com.dmdirc.parser.events.PrivateCTCPEvent;
// import com.dmdirc.parser.events.ChannelCTCPReplyEvent;
// import com.dmdirc.parser.events.PrivateCTCPReplyEvent;
// import com.dmdirc.parser.events.ChannelModeMessageEvent;
// import com.dmdirc.parser.events.ChannelModeNoticeEvent;

import com.dmdirc.parser.events.DataOutEvent;
import net.engio.mbassy.listener.Handler;
import com.dmdirc.parser.irc.IRCParser;
import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;

/**
 * IRC Specific ServerLogger that parses DataOut events into useful events.
 */
public class IRCServerLogger extends ServerLogger {

    /**
     * Create a ServerLogger
     *
     * @param account Account we are logging for.
     * @param connectionHandler IRCConnectionHandler we are logging.
     * @throws Exception if we are unable to create the logs directory.
     */
    public IRCServerLogger(final Account account, final IRCConnectionHandler connectionHandler) throws Exception {
        super(account, connectionHandler);
    }

    @Handler
    public void onDataOut(final DataOutEvent event) {
        final String[] bits = IRCParser.tokeniseLine(event.getData());
        if ((bits[0].equals("PRIVMSG") || bits[0].equals("NOTICE")) && bits.length > 2) {
            final ChannelInfo channel = event.getParser().getChannel(bits[1]);

            boolean isAction = false;
            boolean isCTCP = false;
            String ctcpName = "";

            String message = bits[2];
            String target = bits[1];

            // Used for CTCP/ACTION handling
            String[] messageBits = message.split(" ", 2);

            final Character char1 = (char) 1;
            if ("PRIVMSG".equalsIgnoreCase(bits[0]) && messageBits[0].equalsIgnoreCase(char1 + "ACTION") && Character.valueOf(message.charAt(message.length() - 1)).equals(char1)) {
                isAction = true;
                if (messageBits.length > 1) {
                    message = messageBits[1].substring(0, messageBits[1].length() - 1);
                } else {
                    message = "";
                }
            }

            if (!isAction && Character.valueOf(message.charAt(0)).equals(char1) && Character.valueOf(message.charAt(message.length() - 1)).equals(char1)) {
                isCTCP = true;
                // messageBits is the message been split into 2 parts, the first word and the rest
                // Some CTCPs have messages and some do not
                if (messageBits.length > 1) {
                    message = messageBits[1].substring(0, messageBits[1].length() - 1);
                } else {
                    message = "";
                }
                // Remove the leading char1
                messageBits = messageBits[0].split(char1.toString());
                ctcpName = messageBits[1];
            }

            if (channel != null) {
                final ChannelClientInfo cci = channel.getChannelClient(localClient);
                if (isAction) {
                    handleChannelAction(new ChannelActionEvent(event.getParser(), event.getDate(), channel, cci, message, getFullHostname(cci)));
                } else if (bits[0].equals("PRIVMSG")) {
                    if (isCTCP) {
                        // CTCP
                    } else {
                        handleChannelMessage(new ChannelMessageEvent(event.getParser(), event.getDate(), channel, cci, message, getFullHostname(cci)));
                    }
                } else if (bits[0].equals("NOTICE")) {
                    if (isCTCP) {
                        // CTCP REPLY
                    } else {
                        handleChannelNotice(new ChannelNoticeEvent(event.getParser(), event.getDate(), channel, cci, message, getFullHostname(cci)));
                    }
                }
            } else if (!event.getParser().isValidChannelName(bits[1])) {
                if (isAction) {
                    handleSelfQueryAction(new PrivateActionEvent(event.getParser(), event.getDate(), message, target));
                } else if (bits[0].equals("PRIVMSG")) {
                    if (isCTCP) {
                        // CTCP
                    } else {
                        handleSelfQueryMessage(new PrivateMessageEvent(event.getParser(), event.getDate(), message, target));
                    }
                } else if (bits[0].equals("NOTICE")) {
                    if (isCTCP) {
                        // CTCP REPLY
                    } else {
                        handleSelfQueryNotice(new PrivateNoticeEvent(event.getParser(), event.getDate(), message, target));
                    }
                }
            }
        }
    }

    public void handleSelfQueryAction(final PrivateActionEvent event) {
        final ClientInfo user = event.getParser().getClient(event.getHost());
        final String filename = locator.getLogFile(user);
        if (filename == null) { return; }
        appendLine(filename, "* %s %s", localClient.getNickname(), event.getMessage());
    }

    public void handleSelfQueryMessage(final PrivateMessageEvent event) {
        final ClientInfo user = event.getParser().getClient(event.getHost());
        final String filename = locator.getLogFile(user);
        if (filename == null) { return; }
        appendLine(filename, "<%s> %s", localClient.getNickname(), event.getMessage());
    }

    public void handleSelfQueryNotice(final PrivateNoticeEvent event) {
        final ClientInfo user = event.getParser().getClient(event.getHost());
        final String filename = locator.getLogFile(user);
        if (filename == null) { return; }
        appendLine(filename, "-%s- %s", localClient.getNickname(), event.getMessage());
    }

}

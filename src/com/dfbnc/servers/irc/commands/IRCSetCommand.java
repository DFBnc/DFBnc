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

package com.dfbnc.servers.irc.commands;

import com.dfbnc.commands.AbstractSetCommand;
import com.dfbnc.commands.CommandManager;

/**
 * This file represents the 'IRCSet' command
 */
public class IRCSetCommand extends AbstractSetCommand {
    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"ircset", "*is"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public IRCSetCommand(final CommandManager manager) {
        super(manager);

        setDomain = "irc";

        // Add the valid params
        validParams.put("nickname", new ParamInfo("Nickname to use on IRC", ParamType.WORD, false));
        validParams.put("altnickname", new ParamInfo("Alternative nickname to use if nickname is taken", ParamType.WORD, false));
        validParams.put("realname", new ParamInfo("Realname to use on IRC", ParamType.STRING, false));
        validParams.put("username", new ParamInfo("Username to use on IRC", ParamType.WORD, false));

        validParams.put("bindip", new ParamInfo("IP Address to bind to for new connections", ParamType.WORD, false));

        validParams.put("ratelimit", new ParamInfo("Enable Simple Outbound Rate limiting queue?", ParamType.BOOL, false));
        validParams.put("ratelimittime", new ParamInfo("How long before resetting the item count (in ms)?", ParamType.POSITIVEINT, false));
        validParams.put("ratelimititems", new ParamInfo("How many items in queue before activating rate limiting?", ParamType.POSITIVEINT, false));
        validParams.put("ratelimitwaittime", new ParamInfo("How much time to wait between lines (in ms)?", ParamType.POSITIVEINT, false));

        validParams.put("keepnick", new ParamInfo("Try to keep our nickname (offline if set and offline, else primary).", ParamType.BOOL, false));
        validParams.put("offlinenickname", new ParamInfo("Nickname to change to when offline", ParamType.WORD, false));

        // Currently unimplemented.
        validParams.put("bindipv6", new ParamInfo("[Not Implemented] IPv6 Address to bind to for new connections", ParamType.WORD, false));

        validParams.put("offlineawayreason", new ParamInfo("[Not Implemented] Away reason to set when all users disconnect and we are not away", ParamType.STRING, false));
        validParams.put("offlineawayalways", new ParamInfo("[Not Implemented] Always use the offlineawayreason even if an away reason is already set", ParamType.BOOL, false));

        // Stupid annoying thing that people do...
        validParams.put("partondetach", new ParamInfo("[Not Implemented] Part from all channels on detach then rejoin on attach", ParamType.BOOL, false));

        // Not sure of the point in this one..
        validParams.put("offlineversion", new ParamInfo("[Not Implemented] reply to use for CTCP VERSION queries when no users are connected", ParamType.STRING, false));

        validParams.put("rememberchannels", new ParamInfo("[Not Implemented] Remember active channels between server connections", ParamType.STRING, false));
    }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command lets you manipulate irc settings";
    }
}

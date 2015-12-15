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

package com.dfbnc.commands.user;

import com.dfbnc.commands.AbstractSetCommand;
import com.dfbnc.commands.CommandManager;

/**
 * This file represents the 'ServerSet' command
 */
public class ServerSetCommand extends AbstractSetCommand {
    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"serverset", "*ss"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ServerSetCommand(final CommandManager manager) {
        super(manager);

        setDomain = "server";

        // Add the valid params
        validParams.put("reconnect", new ParamInfo("Auto reconnect on disconnect.", ParamType.BOOL, false));
        validParams.put("reporterrors", new ParamInfo("Report errors from ConnectionHandlers to connected users.", ParamType.BOOL, false));
        validParams.put("autoconnect", new ParamInfo("Auto connect on startup.", ParamType.BOOL, false));
        validParams.put("backbuffer", new ParamInfo("Number of lines to store as backbuffer in each channel (0 to disable).", ParamType.INT, true));
        validParams.put("backbuffertimeout", new ParamInfo("How long in seconds to permit messages to be stored in the backbuffer (0 to disable).", ParamType.INT, true));
        validParams.put("privatebackbuffer", new ParamInfo("Number of lines to store as backbuffer for private messages (0 to disable).", ParamType.INT, true));
        validParams.put("privatebackbuffertimeout", new ParamInfo("How long in seconds to permit messages to be stored in the private message backbuffer (0 to disable).", ParamType.INT, true));
        validParams.put("privatebackbuffertimestamp", new ParamInfo("Force timestamp prepends on private message backbuffers?", ParamType.BOOL, true));
        validParams.put("userdisconnect", new ParamInfo("Disconnect user if the server disconnects us.", ParamType.BOOL, false));
        validParams.put("logging", new ParamInfo("Enable server-side logging of events.", ParamType.BOOL, false));
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
        return "This command lets you manipulate global server-related settings";
    }
}

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
package com.dfbnc.commands.debug;

import com.dfbnc.commands.AdminCommand;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.DebugFlag;
import com.dfbnc.sockets.UserSocket;

import java.util.Arrays;
import java.util.List;

/**
 * This file represents the 'debug logging' command
 */
public class LoggingDebugCommand extends AdminCommand {
    /**
     * Handle a logging command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        if (params.length > 2) {
            final List<String> validParams = Arrays.asList("on", "off");

            final String optionString = getFullParam(output, params, 2, validParams);
            if (optionString == null) { return; }
            if (!validParams.contains(optionString)) {
                output.addBotMessage("Unknown parameter: %s", optionString);
                return;
            }

            output.addBotMessage("This command has been deprecated. Please use 'debug [un]flag dfbnc logging'", params[0]);

            if (user.setDebugFlag(DebugFlag.Logging, optionString.equalsIgnoreCase("on"))) {
                output.addBotMessage("Debug '%s' has been enabled.", DebugFlag.Logging.toString());
            } else {
                output.addBotMessage("Debug '%s' has been disabled.", DebugFlag.Logging.toString());
            }
        } else {
            output.addBotMessage("You need to specify an option 'on' or 'off'");
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"logging"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public LoggingDebugCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command allows you to causing logging to be sent to this socket.";
    }
}

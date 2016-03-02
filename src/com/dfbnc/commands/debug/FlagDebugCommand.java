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

import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.DebugFlag;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.util.Util;

/**
 * This file represents the 'debug flag' command
 */
public class FlagDebugCommand extends Command {
    /**
     * Handle a flag command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {

        if (params.length > 2) {
            final boolean newValue = params[1].equalsIgnoreCase("flag");

            final String setting = Util.joinString(params, " ", 2, 2);

            int count = 0;
            for (final DebugFlag df : DebugFlag.values()) {
                if (!df.toString().toLowerCase().matches(setting.toLowerCase())) { continue; }
                if (df.isAdminOnly() && !user.getAccount().isAdmin()) { continue; }

                if (user.setDebugFlag(df, newValue)) {
                    output.addBotMessage("Debug '%s' has been enabled.", df.toString());
                } else {
                    output.addBotMessage("Debug '%s' has been disabled.", df.toString());
                }
                count++;
            }

            if (count == 0) {
                output.addBotMessage("No debug settings matching: %s", setting);
            }
        } else {
            // List enabled debugs.
            output.addBotMessage("Current debug flags for this socket: ");
            for (DebugFlag df : DebugFlag.values()) {
                if (df.isAdminOnly() && !user.getAccount().isAdmin()) { continue; }
                output.addBotMessage("'%s': %s", df.toString(), (user.debugFlagEnabled(df) ? "Enabled" : "Disabled"));
            }
        }

    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"flag", "unflag"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public FlagDebugCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        if (command.equalsIgnoreCase("flag")) {
            return "This command allows you to enable debug flags on this socket.";
        } else {
            return "This command allows you to disable debug flags on this socket.";
        }
    }
}

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
 *
 */

package com.dfbnc.commands.user;

import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutput;
import com.dfbnc.commands.debug.LogLevelDebugCommand;
import com.dfbnc.commands.debug.LoggingDebugCommand;
import com.dfbnc.commands.debug.RawDebugCommand;
import com.dfbnc.sockets.UserSocket;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * This file represents the 'debug' command
 */
public class DebugCommand extends Command {
    protected CommandManager debugManager = new CommandManager();

    /**
     * Handle a debug command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        String[] actualParams = params;

        if (actualParams.length > 1) {
            final Optional<Entry<String, Command>> matchingCommand =
                    debugManager.getMatchingCommand(actualParams[1], user.getAccount().isAdmin());
            if (matchingCommand.isPresent()) {
                actualParams[1] = matchingCommand.get().getKey();
                matchingCommand.get().getValue().handle(user, actualParams, output);
            } else {
                final Map<String, Command> allCommands = debugManager.getAllCommands(actualParams[1], user.getAccount().isAdmin());
                if (allCommands.size() > 0) {
                    output.sendBotMessage("Multiple possible matches were found for '"+actualParams[1]+"': ");
                    for (String p : allCommands.keySet()) {
                        if (p.charAt(0) == '*') { continue; }
                        output.sendBotMessage("    " + p);
                    }
                } else {
                    output.sendBotMessage("There were no matches for '"+actualParams[1]+"'.");
                    output.sendBotMessage("Try: /dfbnc " + actualParams[0] + " ?");
                }
            }
        } else {
            output.sendBotMessage("You need to choose a debug command to run.");
            output.sendBotMessage("Valid options are:");
            for (Entry<String, Command> e : debugManager.getAllCommands(user.getAccount().isAdmin()).entrySet()) {
                if (e.getKey().charAt(0) != '*') {
                    final String description = e.getValue().getDescription(e.getKey());
                    output.sendBotMessage(String.format("%-20s - %s", e.getKey(), description));
                }
            }
            output.sendBotMessage("Syntax: /dfbnc " + actualParams[0] + " <item> [params]");
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"debug"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public DebugCommand(final CommandManager manager) {
        super(manager);

        debugManager.addCommand(new RawDebugCommand(debugManager));
        debugManager.addCommand(new LoggingDebugCommand(debugManager));
        debugManager.addCommand(new LogLevelDebugCommand(debugManager));
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
        return "Run debugging commands.";
    }

    /**
     * Get detailed help for this command.
     *
     * @param params Parameters the user wants help with.
     *               params[0] will be the command name.
     * @return String[] with the lines to send to the user as the help, or null
     *         if no detailed help is available.
     */
    @Override
    public String[] getHelp(final String[] params) {
        return new String[]{"debug",
                            "This command is used to run debugging commands.",
                            "",
                            "For more information try /dfbnc " + params[0] + " ?",
                           };
    }
}

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
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.commands.show.ConnectionsCommand;
import com.dfbnc.commands.show.FirstTimeCommand;
import com.dfbnc.commands.show.ListUsersCommand;
import com.dfbnc.commands.show.LoggingCommand;
import com.dfbnc.commands.show.ShowCommandsCommand;
import com.dfbnc.commands.show.SystemCommand;
import com.dfbnc.commands.show.VersionCommand;
import com.dfbnc.sockets.UserSocket;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * This file represents the 'show' command
 */
public class ShowCommand extends Command {
    protected CommandManager showManager = new CommandManager();

    /**
     * Handle a show command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        String[] actualParams = params;
        boolean legacy = false;

        // Legacy Support...
        // This may be removed in future.
        if (actualParams[0].equalsIgnoreCase("showcommands")) {
            if (actualParams.length > 1) {
                actualParams = new String[]{"show", "commands", actualParams[1]};
            } else {
                actualParams = new String[]{"show", "commands"};
            }
            legacy = true;
        } else if (actualParams[0].equalsIgnoreCase("lu") || actualParams[0].equalsIgnoreCase("listusers")) {
            actualParams = new String[]{"show", "users"};
            legacy = true;
        } else if (actualParams[0].equalsIgnoreCase("ft") || actualParams[0].equalsIgnoreCase("firsttime")) {
            if (actualParams.length > 1) {
                actualParams = new String[]{"show", "firsttime", actualParams[1]};
            } else {
                actualParams = new String[]{"show", "firsttime"};
            }
            legacy = true;
        } else if (actualParams[0].equalsIgnoreCase("version")) {
            actualParams = new String[]{"show", "version"};
            legacy = true;
        }

        if (legacy) {
            output.addBotMessage("Note: The command '%s' has been deprecated in favour of 'show %s'. Support for legacy command names may be dropped in the future.", params[0], actualParams[1]);
        }

        if (actualParams.length > 1) {
            final Optional<Entry<String, Command>> matchingCommand
                    = showManager.getMatchingCommand(actualParams[1], user.getAccount().isAdmin());
            if (matchingCommand.isPresent()) {
                actualParams[1] = matchingCommand.get().getKey();
                matchingCommand.get().getValue().handle(user, actualParams, output);
            } else {
                final Map<String, Command> allCommands = showManager.getAllCommands(actualParams[1], user.getAccount().isAdmin());
                if (allCommands.size() > 0) {
                    output.addBotMessage("Multiple possible matches were found for '" + actualParams[1] + "': ");
                    for (String p : allCommands.keySet()) {
                        if (p.charAt(0) == '*') { continue; }
                        output.addBotMessage("    " + p);
                    }
                } else {
                    output.addBotMessage("There were no matches for '" + actualParams[1] + "'.");
                    output.addBotMessage("Try: /dfbnc " + actualParams[0] + " ?");
                }
            }
        } else {
            output.addBotMessage("You need to choose something to show.");
            output.addBotMessage("Valid options are:");
            for (Entry<String, Command> e : showManager.getAllCommands(user.getAccount().isAdmin()).entrySet()) {
                if (e.getKey().charAt(0) != '*') {
                    final String description = e.getValue().getDescription(e.getKey());
                    output.addBotMessage(String.format("%-20s - %s", e.getKey(), description));
                }
            }
            output.addBotMessage("Syntax: /dfbnc " + actualParams[0] + " <item> [params]");
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"show", "*showcommands", "*listusers", "*lu", "*version", "*firsttime", "*ft"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ShowCommand(final CommandManager manager) {
        super(manager);

        showManager.addCommand(new ListUsersCommand(showManager));
        showManager.addCommand(new ShowCommandsCommand(showManager));
        showManager.addCommand(new VersionCommand(showManager));
        showManager.addCommand(new FirstTimeCommand(showManager));
        showManager.addCommand(new ConnectionsCommand(showManager));
        showManager.addCommand(new SystemCommand(showManager));
        showManager.addCommand(new LoggingCommand(showManager));
        showManager.addCommand(new Command(showManager){

            @Override
            public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
                   output.addBotMessage("This is not the 'show' you are looking for... ;)");
            }

            @Override
            public String[] handles() { return new String[]{"*running-config", "*startup-config"}; }

            @Override
            public String getDescription(final String command) { return ""; }
        });
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
        return "Show various information about the bnc";
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
        return new String[]{"show",
                            "This command is used to show different information about the BNC.",
                            "",
                            "For more information try /dfbnc " + params[0] + " ?",
                           };
    }
}

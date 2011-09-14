/*
 * Copyright (c) 2006-2007 Shane Mc Cormack
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

package uk.org.dataforce.dfbnc.commands.user;

import java.util.Map;
import java.util.Map.Entry;
import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.commands.CommandNotFoundException;
import uk.org.dataforce.dfbnc.commands.admin.ListUsersCommand;
import uk.org.dataforce.dfbnc.sockets.UserSocket;

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
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        String[] actualParams = params;

        // Legacy Support...
        // This may be removed in future.
        if (actualParams[0].equalsIgnoreCase("showcommands")) {
            if (actualParams.length > 1) {
                actualParams = new String[]{"show", "commands", actualParams[1]};
            } else {
                actualParams = new String[]{"show", "commands"};
            }
        } else if (actualParams[0].equalsIgnoreCase("lu") || actualParams[0].equalsIgnoreCase("listusers")) {
            actualParams = new String[]{"show", "users"};
        } else if (actualParams[0].equalsIgnoreCase("ft") || actualParams[0].equalsIgnoreCase("firsttime")) {
            if (actualParams.length > 1) {
                actualParams = new String[]{"show", "firsttime", actualParams[1]};
            } else {
                actualParams = new String[]{"show", "firsttime"};
            }
        } else if (actualParams[0].equalsIgnoreCase("version")) {
            actualParams = new String[]{"show", "version"};
        }

        if (actualParams.length > 1) {
            final Entry<String, Command> matchingCommand;
            try {
                matchingCommand = showManager.getMatchingCommand(actualParams[1], user.getAccount().isAdmin());
                actualParams[1] = matchingCommand.getKey();
            } catch (CommandNotFoundException cnfe) {
                final Map<String, Command> allCommands = showManager.getAllCommands(actualParams[1], user.getAccount().isAdmin());
                if (allCommands.size() > 0) {
                    user.sendBotMessage("Multiple possible matches were found for '"+actualParams[1]+"': ");
                    for (String p : allCommands.keySet()) {
                        user.sendBotMessage("    " + p);
                    }
                    return;
                } else {
                    user.sendBotMessage("There were no matches for '"+actualParams[1]+"'.");
                    user.sendBotMessage("Try: /dfbnc " + actualParams[0] + " ?");
                    return;
                }
            }

            // Show something!.
            matchingCommand.getValue().handle(user, actualParams);

        } else {
            user.sendBotMessage("You need to choose something to show.");
            user.sendBotMessage("Valid options are:");
            for (Entry<String, Command> e : showManager.getAllCommands(user.getAccount().isAdmin()).entrySet()) {
                if (!e.getKey().startsWith("*")) {
                    final String description = e.getValue().getDescription(e.getKey());
                    user.sendBotMessage(String.format("%-20s - %s", e.getKey(), description));
                }
            }
            user.sendBotMessage("Syntax: /dfbnc " + actualParams[0] + " <item> [params]");
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
        showManager.addCommand(new Command(showManager){

            /** {@inheritDoc} */
            @Override
            public void handle(final UserSocket user, final String[] params) {
                   user.sendBotMessage("This is not the 'show' you are looking for... ;)");
            }

            /** {@inheritDoc} */
            @Override
            public String[] handles() { return new String[]{"*running-config", "*startup-config"}; }

            /** {@inheritDoc} */
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

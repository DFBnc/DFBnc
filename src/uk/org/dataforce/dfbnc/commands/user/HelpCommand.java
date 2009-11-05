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
 * SVN: $Id$
 */
package uk.org.dataforce.dfbnc.commands.user;

import java.util.logging.Level;
import java.util.logging.Logger;
import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.commands.CommandNotFoundException;
import uk.org.dataforce.dfbnc.sockets.UserSocket;


/**
 * This file represents the 'Help' command
 */
public class HelpCommand extends Command {
    /**
     * Handle a Help command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
    
        final String command = (params.length > 1) ? params[1] : "";
    
        if (!command.equals("")) {
            if (user.getAccount() != null) {
                try {
                    final Command cmd = user.getAccount().getCommandManager().getCommand(params[0]);
                    final String[] help = cmd.getHelp(params);
                    if (help != null) {
                        for (String line : help) {
                            user.sendBotMessage(line);
                        }
                    } else {
                        user.sendBotMessage("The command '%s' has no detailed help available.", params[1]);
                    }
                } catch (CommandNotFoundException e) {
                    user.sendBotMessage("The command '%s' does not exist.", params[1]);
                }
            }
        } else {
            //try to execute showcommands, else tell user to do so
            try {
                user.getAccount().getCommandManager().getCommand("showcommands").handle(user, params);
            } catch (CommandNotFoundException ex) {
                user.sendBotMessage("You need to specify a command to get help for, try 'showcommands' to see all the commands");
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
        return new String[]{"help"};
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
        return "This command shows more detailed help for commands";
    }
    
    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public HelpCommand (final CommandManager manager) { super(manager); }
    
    /**
     * Get SVN Version information.
     *
     * @return SVN Version String
     */
    public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }    
}
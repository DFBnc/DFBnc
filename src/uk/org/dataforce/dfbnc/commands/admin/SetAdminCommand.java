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
package uk.org.dataforce.dfbnc.commands.admin;

import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.UserSocket;
import uk.org.dataforce.dfbnc.Account;
import uk.org.dataforce.dfbnc.AccountManager;

/**
 * This file represents the 'SetAdmin' command
 */
public class SetAdminCommand extends Command {
    /**
     * Handle a SetAdmin command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        if (params.length == 1) {
            user.sendBotMessage("You need to specify a username to chagne the admin status of.");
        } else {
            final String account = params[1];
            if (!AccountManager.exists(account)) {
                user.sendBotMessage("No account with the name '%s' exists.", account);
            } else {
                final Account acc = AccountManager.get(account);
                if (acc == user.getAccount()) {
                    user.sendBotMessage("You can't change your own status");
                } else {
                    final String newStatus = (params.length > 2) ? params[2] : "";
                    if (newStatus.isEmpty()) {
                        if (acc.isAdmin()) {
                            user.sendBotMessage("The account '%s' is currently an admin.", account);
                        } else {
                            user.sendBotMessage("The account '%s' is not currently an admin.", account);
                        }
                    } else {
                        acc.setAdmin(newStatus.equalsIgnoreCase("true") || newStatus.equalsIgnoreCase("yes") || newStatus.equalsIgnoreCase("on") || newStatus.equalsIgnoreCase("1"));
                        if (acc.isAdmin()) {
                            user.sendBotMessage("The account '%s' is now an admin.", account);
                        } else {
                            user.sendBotMessage("The account '%s' is now no longer an admin.", account);
                        }
                    }
                }
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
        return new String[]{"setadmin"};
    }
    
    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public SetAdminCommand (final CommandManager manager) { super(manager); }
    
    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command will let you change the admin status of a user on the BNC";
    }
    
    /**
     * Get SVN Version information.
     *
     * @return SVN Version String
     */
    public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }    
}
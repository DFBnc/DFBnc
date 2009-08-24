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
import uk.org.dataforce.dfbnc.sockets.UserSocket;
import uk.org.dataforce.dfbnc.AccountManager;

/**
 * This file represents the 'DelUser' command
 */
public class DelUserCommand extends Command {
    /**
     * Handle a DelUser command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        if (params.length == 1) {
            user.sendBotMessage("You need to specify a username to delete.");
        } else {
            final String account = params[1];
            if (AccountManager.exists(account)) {
                if (params.length == 2) {
                    final String deleteCode = AccountManager.makePassword(15);
                    AccountManager.get(account).setDeleteCode(deleteCode);
                    user.sendBotMessage("Deleting an account will remove all settings for the account.");
                    user.sendBotMessage("Are you sure you want to continue?");
                    user.sendBotMessage("To continue please enter /dfbnc -BNC %s %s %s", params[0], account, deleteCode);
                } else if (!params[2].equals(AccountManager.get(account).getDeleteCode())) {
                    user.sendBotMessage("Invalid Delete code specified.");
                } else {
                    user.sendBotMessage("Deleting account '%s'", account);
                    AccountManager.get(account).delete();
                }
            } else {
                user.sendBotMessage("An account with the name '%s' does not exist.", account);
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
        return new String[]{"deluser"};
    }
    
    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public DelUserCommand (final CommandManager manager) { super(manager); }
    
    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command will let you delete a user from the BNC";
    }
    
    /**
     * Get SVN Version information.
     *
     * @return SVN Version String
     */
    public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }    
}
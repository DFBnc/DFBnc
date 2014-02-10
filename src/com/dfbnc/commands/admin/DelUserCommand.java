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
 */
package com.dfbnc.commands.admin;

import java.io.IOException;
import com.dfbnc.commands.AdminCommand;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.AccountManager;
import com.dfbnc.commands.CommandOutput;

/**
 * This file represents the 'DelUser' command
 */
public class DelUserCommand extends AdminCommand {
    /**
     * Handle a DelUser command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        if (params.length == 1) {
            output.sendBotMessage("You need to specify a username to delete.");
        } else {
            final String account = params[1];
            if (AccountManager.exists(account)) {
                if (params.length == 2) {
                    final String deleteCode = AccountManager.makePassword(15);
                    AccountManager.get(account).setDeleteCode(deleteCode);
                    output.sendBotMessage("Deleting an account will remove all settings for the account.");
                    output.sendBotMessage("Are you sure you want to continue?");
                    output.sendBotMessage("To continue please enter /raw dfbnc %s %s %s", params[0], account, deleteCode);
                } else if (!params[2].equals(AccountManager.get(account).getDeleteCode())) {
                    output.sendBotMessage("Invalid Delete code specified.");
                } else {
                    output.sendBotMessage("Deleting account '%s'", account);
                    try {
                        AccountManager.get(account).delete();
                        output.sendBotMessage("Account '%s' deleted.", account);
                    } catch (final IOException ioe) {
                        output.sendBotMessage("Deleting account '%s' failed: %s", account, ioe);
                    }
                }
            } else {
                output.sendBotMessage("An account with the name '%s' does not exist.", account);
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
}
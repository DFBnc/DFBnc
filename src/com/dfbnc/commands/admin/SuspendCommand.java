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
package com.dfbnc.commands.admin;

import com.dfbnc.Account;
import com.dfbnc.DFBnc;
import com.dfbnc.commands.AdminCommand;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.UserSocket;

/**
 * This file represents the 'Suspend' command
 */
public class SuspendCommand extends AdminCommand {
    /**
     * Handle an Suspend command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        if (params.length == 1) {
            output.addBotMessage("You need to specify a username to suspend.");
        } else {
            if (user.isReadOnly()) {
                output.addBotMessage("Sorry, read-only sub-clients are unable to suspend users.");
                return;
            }

            final String account = params[1];
            if (!DFBnc.getAccountManager().exists(account)) {
                output.addBotMessage("No account with the name '%s' exists.", account);
            } else {
                final Account acc = DFBnc.getAccountManager().get(account);
                if (acc == user.getAccount()) {
                    output.addBotMessage("You can't suspend yourself.");
                } else if (acc.isSuspended()) {
                    output.addBotMessage("The Account '%s' is already suspended (%s).", account, acc.getSuspendReason());
                } else {
                    final StringBuilder reason = new StringBuilder();
                    for (int i = 2; i < params.length; i++) { reason.append(params[i]); }
                    output.addBotMessage("Suspending Account '%s'..", account);
                    acc.setSuspended(true, reason.toString());
                    output.addBotMessage("Account suspended with reason: %s", acc.getSuspendReason());
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
        return new String[]{"suspend"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public SuspendCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command will let you suspend a user on the BNC";
    }
}

/*
 * Copyright (c) 2006-2013 Shane Mc Cormack, Gregory Holmes
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
package com.dfbnc.commands.show;

import com.dfbnc.Account;
import com.dfbnc.DFBnc;
import com.dfbnc.commands.AdminCommand;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.UserSocket;

import java.util.Collection;

/**
 * Shows a list of users known to the bouncer.
 */
public class ListUsersCommand extends AdminCommand {

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ListUsersCommand(final CommandManager manager) {
        super(manager);
    }

    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        final Collection<Account> accounts = DFBnc.getAccountManager().getAccounts();
        output.addBotMessage("This BNC has %s users: ", accounts.size());
        for (Account account : accounts) {
            final StringBuilder sb = new StringBuilder("    ");

            if (account.equals(user.getAccount())) { sb.append((char)2); }
            sb.append(account.getName());
            if (account.equals(user.getAccount())) { sb.append((char)2); }

            if (account.isAdmin()) {
                sb.append("  [Admin]");
            }

            if (account.isSuspended()) {
                sb.append("  [Suspended: ");
                sb.append(account.getSuspendReason());
                sb.append("]");
            }

            if (account.getUserSockets().size() > 0) {
                sb.append("  (Currently connected)");
            }

            output.addBotMessage("%s", sb.toString());
        }
    }

    @Override
    public String[] handles() {
        return new String[]{"users",};
    }

    @Override
    public String getDescription(String command) {
        return "Lists the list of known users.";
    }
}

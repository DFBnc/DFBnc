/*
 * Copyright (c) 2006-2007 Shane Mc Cormack, Gregory Holmes
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
package uk.org.dataforce.dfbnc.commands.admin;

import uk.org.dataforce.dfbnc.Account;
import uk.org.dataforce.dfbnc.AccountManager;
import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.sockets.UserSocket;

/**
 * Shows a list of users known to the bouncer.
 */
public class ListUsersCommand extends Command {

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ListUsersCommand(final CommandManager manager) {
        super(manager);
    }

    /** {@inheritDoc} */
    @Override
    public void handle(UserSocket user, String[] params) {
        user.sendBotMessage("Users: ");
        for (Account account : AccountManager.getAccounts()) {
            user.sendBotMessage("Username: %s", account.getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] handles() {
        return new String[]{"listusers", "lu",};
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription(String command) {
        return "Lists the list of known users.";
    }
}

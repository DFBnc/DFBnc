/*
 * Copyright (c) 2006-2016 DFBnc Developers
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
package com.dfbnc.commands.user;

import com.dfbnc.DFBnc;
import com.dfbnc.authentication.AuthProvider;
import com.dfbnc.authentication.AuthProviderManager;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.AbstractListEditCommand;
import com.dfbnc.commands.ListOption;
import com.dfbnc.config.Config;
import com.dfbnc.sockets.UserSocket;

/**
 * This file represents the 'AuthList' command
 */
public class AuthListCommand extends AbstractListEditCommand {

    @Override
    public String getPropertyName(final String command) { return "authlist"; }

    @Override
    public String getDomainName(final String command) { return "user"; }

    @Override
    public String getListName(final String command) { return "Auth List" + (!command.equalsIgnoreCase("--global") ? " (For Sub-Client: " + command + ")" : ""); }

    @Override
    public ListOption checkItem(final String command, final String input, final UserSocket user) {
        final String[] bits = input.split(" ", 2);
        final AuthProviderManager apm = DFBnc.getAuthProviderManager();
        if (apm.hasProvider(bits[0])) {
            final AuthProvider ap = apm.getProvider(bits[0]);

            final String params = ap.validateParams(user, (command.equalsIgnoreCase("--global") ? "" : command), (bits.length > 1 ? bits[1] : ""));

            if (!params.isEmpty()) {
                return new ListOption(true, ap.getProviderName() + " " + params, null);
            } else {
                return new ListOption(false, input, new String[]{"Invalid parameters for provider.", "Expected: " + ap.getProviderName() + " " + ap.getExpectedParams()});
            }
        }

        return new ListOption(false, input, new String[]{"No such provider '" + bits[0] + "'"});
    }

    @Override
    public String[] getUsageOutput(final String command) {
        if (command.equalsIgnoreCase("add")) {
            return new String[]{"You must specify an authentication method."};
        } else if (command.equalsIgnoreCase("edit")) {
            return new String[]{"You must specify a position number to edit, and an authentication method."};
        } else if (command.equalsIgnoreCase("ins")) {
            return new String[]{"You must specify a position to insert this item, and an authentication method."};
        } else {
            return new String[]{""};
        }
    }

    @Override
    public String getAddUsageSyntax() {
        return "<METHOD> <Parameters>";
    }

    @Override
    public boolean canAdd(final String command) { return true; }

    @Override
    public boolean hasSubList() {
        return true;
    }

    @Override
    public boolean allowGlobalList() {
        return true;
    }

    @Override
    public Config getConfig(final UserSocket user, final String sublist) {
        return user.getAccount().getConfig(sublist);
    }

    @Override
    public boolean isDynamicList() {
        return true;
    }

    @Override
    public String validSubList(final String command) {
        return command.toLowerCase();
    }

    @Override
    public String[] handles() {
        return new String[]{"authlist", "*al"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public AuthListCommand (final CommandManager manager) { super(manager); }

    @Override
    public String getDescription(final String command) {
        return "This command lets you manipulate the authlist";
    }
}

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
package com.dfbnc.servers.irc.commands;

import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.AbstractListEditCommand;
import com.dfbnc.commands.ListOption;
import com.dfbnc.servers.irc.IRCServerType;

/**
 * This file represents the 'ServerList' command
 */
public class ServerListCommand extends AbstractListEditCommand {
    /**
     * Get the name of the property to store the list in.
     *
     * @param command The command passed as param[0]
     * @return The name of the property to store the list in.
     */
    @Override
    public String getPropertyName(final String command) { return "serverlist"; };

    /**
     * Get the name of the domain to store the list in.
     *
     * @param command The command passed as param[0]
     * @return The name of the domain to store the list in.
     */
    @Override
    public String getDomainName(final String command) { return "irc"; };

    /**
     * Get the name of the list.
     * This is used in various outputs from the command.
     *
     * @param command The command passed as param[0]
     * @return The name of the list
     */
    @Override
    public String getListName(final String command) { return "Server list"; }

    /**
     * Check an item.
     * This should return a ListOption for the given input.
     *
     * @param command The command passed as param[0]
     * @param input The input to validate
     * @return ListOption for this parameter.
     */
    @Override
    public ListOption checkItem(final String command, final String input) {
        String[] inputBits = IRCServerType.parseServerString(input);
        return new ListOption(true, inputBits[3], null);
    }

    /**
     * Get the output to give for an "add", "edit" or "ins" request without sufficient parameters
     *
     * @param command Command to get usage info for (add, edit, ins)
     * @return The output to give
     */
    @Override
    public String[] getUsageOutput(final String command) {
        if (command.equalsIgnoreCase("add")) {
            return new String[]{"You must specify a server to add in the format: <server>[:port] [password]",
                                "Prefixing the port with + signifies an SSL connection"
                               };
        } else if (command.equalsIgnoreCase("edit")) {
            return new String[]{"You must specify a position number to edit, and a server to add in the format: <number> <server>[:[+]port] [password]",
                                "Prefixing the port with + signifies an SSL connection"
                               };
        } else if (command.equalsIgnoreCase("ins")) {
            return new String[]{"You must specify a position to insert this item, and a server to add in the format: <number> <server>[:[+]port] [password]",
                                "Prefixing the port with + signifies an SSL connection"
                               };
        } else {
            return new String[]{""};
        }
    }

    /**
     * Get the output to give for the syntax to add/edit commands to show valid input
     *
     * @return The output to give for the syntax to add/edit commands to show valid input
     */
    @Override
    public String getAddUsageSyntax() {
        return "<Server>[:[+]Port] [password]";
    }

    /**
     * Can this list be added to?
     * (This also disables edit and insert)
     *
     * @param command Command to get output for
     * @return If this list can be added to.
     */
    @Override
    public boolean canAdd(final String command) { return true; }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"serverlist", "*sl"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ServerListCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command lets you manipulate the irc server list";
    }
}
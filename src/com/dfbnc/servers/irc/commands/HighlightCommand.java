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
import com.dfbnc.config.Config;
import com.dfbnc.sockets.UserSocket;

/**
 * This file represents the 'Highlight' command
 */
public class HighlightCommand extends AbstractListEditCommand {

    @Override
    public String getPropertyName(final String command) { return "highlight"; }

    @Override
    public String getDomainName(final String command) { return "irc"; }

    @Override
    public String getListName(final String command) { return "Highlight List (For Sub-Client: " + command + ")"; }

    @Override
    public ListOption checkItem(final String command, final String input) {
        return new ListOption(true, input, null);
    }

    @Override
    public String[] getUsageOutput(final String command) {
        if (command.equalsIgnoreCase("add")) {
            return new String[]{"You must specify a regex to highlight on."};
        } else if (command.equalsIgnoreCase("edit")) {
            return new String[]{"You must specify a position number to edit, and a regex to highlight on."};
        } else if (command.equalsIgnoreCase("ins")) {
            return new String[]{"You must specify a position to insert this item, and a regex to highlight on."};
        } else {
            return new String[]{""};
        }
    }

    @Override
    public String getAddUsageSyntax() {
        return "<regex>";
    }

    @Override
    public boolean canAdd(final String command) { return true; }

    @Override
    public boolean hasSubList() {
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
        return new String[]{"highlight"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public HighlightCommand (final CommandManager manager) { super(manager); }

    @Override
    public String getDescription(final String command) {
        return "This command lets you manipulate the highlight list";
    }
}
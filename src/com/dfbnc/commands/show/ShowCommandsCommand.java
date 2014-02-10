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
package com.dfbnc.commands.show;

import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutput;
import com.dfbnc.sockets.UserSocket;

import java.util.TreeMap;
import java.util.SortedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

/**
 * This file represents the 'ShowCommands' command
 */
public class ShowCommandsCommand extends Command {
    /**
     * Handle a ShowCommands command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        // This stores the output for any admin commands we run across, these are
        // displayed at the end after the normal-user commands.
        ArrayList<String> adminCommands = new ArrayList<>();

        final String commandsType = getFullParam(output, params, 2, Arrays.asList("all", "admin", "user", ""));
        if (commandsType == null) { return; }

        if (commandsType.equals("") || commandsType.equalsIgnoreCase("all") || commandsType.equalsIgnoreCase("user")) {
            output.sendBotMessage("The following commands are available to you:");
            output.sendBotMessage("");
        }

        CommandManager cmdmgr = user.getAccount().getCommandManager();
        SortedMap<String, Command> commands = new TreeMap<>(cmdmgr.getAllCommands(user.getAccount().isAdmin()));
        for (Entry<String, Command> entry : commands.entrySet()) {
            if (entry.getKey().charAt(0) == '*') { continue; }
            final Command command = entry.getValue();
            if (command.isAdminOnly()) {
                adminCommands.add(String.format("%-20s - %s", entry.getKey(), command.getDescription(entry.getKey())));
            } else if (commandsType.equals("") || commandsType.equalsIgnoreCase("all") || commandsType.equalsIgnoreCase("user")) {
                output.sendBotMessage(String.format("%-20s - %s", entry.getKey(), command.getDescription(entry.getKey())));
            }
        }


        if (commandsType.equals("") || commandsType.equalsIgnoreCase("all") || commandsType.equalsIgnoreCase("admin")) {
            if (user.getAccount().isAdmin()) {
                if (adminCommands.size() > 0) {
                    if (commandsType.equals("") || commandsType.equalsIgnoreCase("all") || commandsType.equalsIgnoreCase("user")) {
                        output.sendBotMessage("");
                    }
                    if (commandsType.equalsIgnoreCase("admin")) {
                        output.sendBotMessage("The following admin-only commands are available to you:");
                    } else {
                        output.sendBotMessage("The following admin-only commands are also available to you:");
                    }
                    output.sendBotMessage("");
                    for (final String out : adminCommands) {
                        output.sendBotMessage(out);
                    }
                } else {
                    output.sendBotMessage("There are no admin-only commands available to you.");
                }
            } else if (commandsType.equalsIgnoreCase("admin")) {
                output.sendBotMessage("Admin commands are not available to you.");
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
        return new String[]{"commands"};
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
        return "This command shows what commands are available to you";
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ShowCommandsCommand (final CommandManager manager) { super(manager); }
}
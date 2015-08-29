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
 *
 */

package com.dfbnc.commands.user;

import com.dfbnc.AccountManager;
import com.dfbnc.DFBnc;
import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutput;
import com.dfbnc.sockets.UserSocket;

/**
 * This file represents the 'password' command
 */
public class SaveCommand extends Command {

    /**
     * Handle a save command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        output.sendBotMessage("Saving config files...");
        AccountManager.saveAccounts();
        output.sendBotMessage("Done.");
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"saveconfig", "*sc"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public SaveCommand(final CommandManager manager) {
        super(manager);
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
        return "Saves a users config.";
    }

    /**
     * Get detailed help for this command.
     *
     * @param params Parameters the user wants help with.
     *               params[0] will be the command name.
     * @return String[] with the lines to send to the user as the help, or null
     *         if no detailed help is available.
     */
    @Override
    public String[] getHelp(final String[] params) {
        return new String[]{ "saveconfig", "Saves your user config" };
    }
}

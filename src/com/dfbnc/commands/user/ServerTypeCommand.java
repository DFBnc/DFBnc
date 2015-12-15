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
package com.dfbnc.commands.user;

import com.dfbnc.DFBnc;
import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.servers.ServerType;
import com.dfbnc.servers.ServerTypeNotFound;
import com.dfbnc.sockets.UserSocket;

import java.util.Arrays;
import java.util.Collection;

/**
 * This file represents the 'ServerType' command
 */
public class ServerTypeCommand extends Command {
    /**
     * Handle a ServerType command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        String[] actualParams = params;

        if (actualParams.length > 1) {
            actualParams[1] = getFullParam(output, actualParams, 1, Arrays.asList("settype", "help"));
            if (actualParams[1] == null) { return; }
        }

        if (actualParams.length > 1 && actualParams[1].equalsIgnoreCase("settype")) {
            if (actualParams.length > 2) {
                if (user.isReadOnly()) {
                    output.addBotMessage("Sorry, read-only sub-clients are unable to make changes to the server type.");
                    return;
                }

                final Collection<String> availableTypes = DFBnc.getServerTypeManager().getServerTypeNames();
                availableTypes.add("none");
                actualParams[2] = getFullParam(output, actualParams, 2, availableTypes);
                if (actualParams[2] == null) { return; }

                final ServerType currentType = user.getAccount().getServerType();
                if (actualParams[2].equalsIgnoreCase("none")) {
                    user.getAccountConfig().setOption("server", "servertype", "");
                    output.addBotMessage("You now have no servertype.");
                    if (currentType != null) { currentType.deactivate(user.getAccount()); }
                } else {
                    try {
                        ServerType serverType = DFBnc.getServerTypeManager().getServerType(actualParams[2]);
                        if (currentType != null) { currentType.deactivate(user.getAccount()); }
                        serverType.activate(user.getAccount());
                        user.getAccountConfig().setOption("server", "servertype", actualParams[2].toLowerCase());
                        output.addBotMessage("Your ServerType is now %s.", actualParams[2].toLowerCase());
                    } catch (ServerTypeNotFound e) {
                        output.addBotMessage("Sorry, %s", e);
                    }
                }
            } else {
                output.addBotMessage("Available Types:");
                for (String name : DFBnc.getServerTypeManager().getServerTypeNames()) {
                    try {
                        final ServerType type = DFBnc.getServerTypeManager().getServerType(name);
                        output.addBotMessage("    %s - %s", name, type.getDescription());
                    } catch (ServerTypeNotFound ex) { /* It will be found. */ }
                }
            }
        } else if (actualParams.length > 1 && actualParams[1].equalsIgnoreCase("help")) {
            output.addBotMessage("This command allows you to set the servertype for this account.");
            final String currentType = user.getAccountConfig().getOption("server", "servertype");
            String info = "";
            final ServerType st = user.getAccount().getServerType();
            if (st == null) {
                info = " (Currently not available)";
            }
            output.addBotMessage("Your current servertype is: %s%s", currentType, info);
            output.addBotMessage("");
            output.addBotMessage("You can set your type using the command: /dfbnc %s settype <type>", actualParams[0]);
            output.addBotMessage("A list of available types can be seen by ommiting the <type> param");
        } else {
            output.addBotMessage("For usage information use /dfbnc %s help", actualParams[0]);
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"servertype", "*st"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ServerTypeCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command changes your ServerType";
    }
}

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
package com.dfbnc.commands.user;

import com.dfbnc.Account;
import com.dfbnc.ConnectionHandler;
import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.sockets.UnableToConnectException;


/**
 * This file represents the '[re|dis|]connect'  commands
 */
public class ConnectCommand extends Command {
    /**
     * Handle a Connect command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        final Account acc = user.getAccount();

        if (!params[0].equalsIgnoreCase("connect")) {
            output.addBotMessage("Disconnecting...");
            if (acc.getConnectionHandler() == null) {
                output.addBotMessage("Not connected!");
                if (acc.isReconnecting()) {
                    acc.cancelReconnect();
                    output.addBotMessage("Reconnect attempt cancelled.");
                }
            } else {
                String reason = "BNC Disconnecting";
                if (params.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < params.length; ++i) { sb.append(params[i]).append(" "); }
                    reason = sb.toString().trim();
                } else if (params[0].equalsIgnoreCase("reconnect")) {
                    reason = "Reconnecting";
                }
                acc.disableReconnect();
                acc.getConnectionHandler().shutdown(reason);
                acc.setConnectionHandler(null);
            }
            if (!params[0].equalsIgnoreCase("reconnect")) { return; }
        }

        if (acc.getConnectionHandler() == null) {
            output.addBotMessage("Connecting...");
            try {
                ConnectionHandler handler = acc.getServerType().newConnectionHandler(user.getAccount(), -1);
                acc.setConnectionHandler(handler);
            } catch (UnableToConnectException utce) {
                output.addBotMessage("There was an error connecting: %s", utce.getMessage());
            }
        } else {
            output.addBotMessage("Already connected.");
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"connect", "reconnect", "disconnect", "*quit"};
    }

    @Override
    public boolean allowShort(final String handle) {
        // Only allow the connect command to be shortened.
        return handle.equalsIgnoreCase("connect");
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ConnectCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        if (command.equalsIgnoreCase("reconnect")) {
            return "This command lets you disconnect the current session, then reconnect";
        } else if (command.equalsIgnoreCase("disconnect") || command.equalsIgnoreCase("quit")) {
            return "This command lets you disconnect the current session";
        } else {
            return "This command lets you connect to a Server if not already connected";
        }
    }
}
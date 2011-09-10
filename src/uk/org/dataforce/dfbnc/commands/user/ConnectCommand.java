/*
 * Copyright (c) 2006-2007 Shane Mc Cormack
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
 * SVN: $Id$
 */
package uk.org.dataforce.dfbnc.commands.user;

import uk.org.dataforce.dfbnc.ConnectionHandler;
import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.sockets.UserSocket;
import uk.org.dataforce.dfbnc.sockets.UnableToConnectException;


/**
 * This file represents the '[re|dis|]connect'  commands
 */
public class ConnectCommand extends Command {
    /**
     * Handle a Connect command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        user.sendBotMessage("----------------");
        if (!params[0].equalsIgnoreCase("connect")) {
            user.sendBotMessage("Disconnecting...");
            if (user.getAccount().getConnectionHandler() == null) {
                user.sendBotMessage("Not connected!");
            } else {
                String reason = "BNC Disconnecting";
                if (params.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < params.length; ++i) { sb.append(params[i]+" "); }
                    reason = sb.toString().trim();
                } else if (params[0].equalsIgnoreCase("reconnect")) {
                    reason = "Reconnecting";
                }
                user.getAccount().getConnectionHandler().shutdown(reason);
                user.getAccount().setConnectionHandler(null);
            }
            if (!params[0].equalsIgnoreCase("reconnect")) { return; }
        }
        
        if (user.getAccount().getConnectionHandler() == null) {
            user.sendBotMessage("Connecting...");
            try {
                ConnectionHandler handler = user.getAccount().getServerType().newConnectionHandler(user.getAccount(), -1);
                user.getAccount().setConnectionHandler(handler);
            } catch (UnableToConnectException utce) {
                user.sendBotMessage("There was an error connecting: "+utce.getMessage());
            }
        } else {
            user.sendBotMessage("Already connected.");
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
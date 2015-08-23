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

import java.util.Arrays;
import java.util.List;
import com.dfbnc.commands.Command;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutput;
import com.dfbnc.sockets.UserSocket;

/**
 * This file represents the 'Sessions' command
 */
public class SessionsCommand extends Command {
    /**
     * Handle a Sessions command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        final List<String> validParams = Arrays.asList("all", "authenticated", "unauthenticated", "account", "");
        final String optionString = getFullParam(output, params, 2, validParams);
        if (optionString == null) { return; }

        output.sendBotMessage("WARNING: This command is deprecated, please use 'show connections' instead.");

        if (!validParams.contains(optionString)) {
            output.sendBotMessage("Unknown session type: %s", optionString);
            return;
        }

        if (user.getAccount().isAdmin() && !optionString.equalsIgnoreCase("")) {
            output.sendBotMessage("Currently connected sockets (Type: %s):", optionString);
            output.sendBotMessage("");
            int count = 0;
            int matched = 0;
            for (final UserSocket u : UserSocket.getUserSockets()) {
                count++;
                final StringBuilder sb = new StringBuilder();

                sb.append(u.getInfo());
                sb.append(" - ");
                sb.append(u.getSocketID());
                sb.append(" - ");
                if (u.getAccount() == null) {
                    if (!optionString.equalsIgnoreCase("all") && !optionString.equalsIgnoreCase("unauthenticated")) { continue; }
                    sb.append("UNAUTHENTICATED");
                } else {
                    if (optionString.equalsIgnoreCase("account") && !u.getAccount().equals(user.getAccount())) { continue; }
                    if (optionString.equalsIgnoreCase("unauthenticated")) { continue; }
                    sb.append(u.getAccount().getName());
                    if (u.getAccount().isAdmin()) { sb.append('*'); }
                }
                if (u.getClientID() != null) {
                    sb.append(" [");
                    sb.append(u.getClientID());
                    sb.append("]");
                }
                if (u.getClientVersion() != null) {
                    sb.append(" \"");
                    sb.append(u.getClientVersion());
                    sb.append("\"");
                }
                sb.append(" - Client Type: ");
                sb.append(u.getClientType());

                sb.append("     (");
                sb.append(u.toString());
                sb.append(")");

                output.sendBotMessage(sb.toString());
                matched++;
            }
            output.sendBotMessage("----------");
            output.sendBotMessage("Matched: %d    Total: %d", matched, count);
        } else {
            output.sendBotMessage("Currently connected sockets for this account:");
            output.sendBotMessage("");
            int count = 0;
            for (final UserSocket u : user.getAccount().getUserSockets()) {
                count++;
                final StringBuilder sb = new StringBuilder();
                sb.append(u.getIP());
                if (u.getClientID() != null) {
                    sb.append(" [");
                    sb.append(u.getClientID());
                    sb.append("]");
                }
                if (u.getClientVersion() != null) {
                    sb.append(" - \"");
                    sb.append(u.getClientVersion());
                    sb.append("\"");
                }
                sb.append(" - Client Type: ");
                sb.append(u.getClientType());

                output.sendBotMessage(sb.toString());
            }
            output.sendBotMessage("----------");
            output.sendBotMessage("Total: %d", count);
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"sessions"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public SessionsCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command gives information on currently connected sessions.";
    }
}

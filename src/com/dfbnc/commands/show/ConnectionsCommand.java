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
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * This file represents the 'Connections' command
 */
public class ConnectionsCommand extends Command {
    /**
     * Handle a Connections command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        final List<String> validParams;
        if (user.getAccount().isAdmin()) {
            validParams = Arrays.asList("account", "all", "authenticated", "unauthenticated", "brief", "full", "");
        } else {
            validParams = Arrays.asList("account", "brief", "full", "");
        }

        final Set<String> askedParams = new HashSet<>();

        for (int i = 2; i < params.length; i++) {
            final String optionString = getFullParam(output, params, i, validParams);
            if (optionString == null) { return; }
            if (!validParams.contains(optionString)) {
                output.sendBotMessage("Unknown parameter: %s", optionString);
                return;
            }

            askedParams.add(optionString);
        }

        // If nothing specified, or non-admin, force "account"
        if (askedParams.isEmpty() || !user.getAccount().isAdmin()) { askedParams.add("account"); }

        // Must specify either authenticated, unauthenticated or both
        if (!askedParams.contains("authenticated") && !askedParams.contains("unauthenticated")) { askedParams.add("authenticated"); }

        // Must specify either brief or full
        if (!askedParams.contains("brief") && !askedParams.contains("full")) { askedParams.add("full"); }

        // If all, set the right flags
        if (askedParams.contains("all")) {
            askedParams.remove("account");
            askedParams.remove("all");
            askedParams.add("authenticated");
            askedParams.add("unauthenticated");
        }

        // Check for invalid combinations
        if ((askedParams.contains("brief") && askedParams.contains("full")) || (askedParams.contains("unauthenticated") && askedParams.contains("account"))) {
            output.sendBotMessage("Invalid combination of parameters.");
            return;
        }

        output.sendBotMessage("Currently connected sockets:");
        if (user.getAccount().isAdmin()) {
            output.sendBotMessage("    Filter: %s", Arrays.toString(askedParams.toArray()));
        }
        output.sendBotMessage("");

        int count = 0;
        int matched = 0;
        for (final UserSocket u : UserSocket.getUserSockets()) {
            count++;

            if (askedParams.contains("account") && user.getAccount().equals(u.getAccount()) == false) { continue; }
            if (!askedParams.contains("authenticated") && u.getAccount() != null) { continue; }
            if (!askedParams.contains("unauthenticated") && u.getAccount() == null) { continue; }

            if (askedParams.contains("full")) {
                if (u.equals(user)) {
                    output.sendBotMessage("User: " + u.getSocketID() + " **Current Socket**");
                } else {
                    output.sendBotMessage("User: " + u.getSocketID());
                }
                output.sendBotMessage("          Socket ID: " + u.toString());
                if (u.getAccount() == null) {
                    output.sendBotMessage("          Account: UNAUTHENTICATED");
                } else {
                    output.sendBotMessage("          Account: " + u.getAccount().getName());
                    if (u.getAccount().isAdmin()) {
                        output.sendBotMessage("          User is admin");
                    }
                }
                if (u.getClientID() != null) {
                    output.sendBotMessage("          Sub-Client: " + u.getClientID());
                }
                if (u.getClientVersion() != null) {
                    output.sendBotMessage("          Client Version: " + u.getClientVersion());
                }
                output.sendBotMessage("          Client Type: " + u.getClientType());

                final InetSocketAddress remote = u.getRemoteSocketAddress();
                final InetSocketAddress local = u.getLocalSocketAddress();

                output.sendBotMessage("          UserSocket Info: ");
                output.sendBotMessage("                    Remote IP: " + remote.getAddress());
                output.sendBotMessage("                    Remote Port: " + remote.getPort());
                output.sendBotMessage("                    Local IP: "  + local.getAddress());
                output.sendBotMessage("                    Local Port: " + local.getPort());
                output.sendBotMessage("                    SSL: " + Boolean.toString(u.isSSL()));
                output.sendBotMessage("");
            } else {
                final String acc = (u.getAccount() == null ? "UNAUTHENTICATED" : u.getAccount().getName());
                final String subclient = (u.getClientID() != null ? "+" + u.getClientID() : "");
                final String current = (u.equals(user) ? "**" : "");
                final String admin = (u.getAccount() != null && u.getAccount().isAdmin() ? "@" : "");
                output.sendBotMessage("[%s%s%s%s] - {%s} - %s: %s", admin, acc, subclient, current, u.getInfo(), u.getClientType(), u.getClientVersion());
            }
            matched++;
        }
        output.sendBotMessage("----------");
        if (user.getAccount().isAdmin()) {
            output.sendBotMessage("Matched: %d    Total: %d", matched, count);
        } else {
            output.sendBotMessage("Total: %d", matched);
        }
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"connections", "*sessions"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public ConnectionsCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command gives information on current client connections.";
    }
}

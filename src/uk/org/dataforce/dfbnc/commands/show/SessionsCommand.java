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
package uk.org.dataforce.dfbnc.commands.show;

import java.util.Arrays;
import java.util.List;
import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.sockets.UserSocket;

/**
 * This file represents the 'Sessions' command
 */
public class SessionsCommand extends Command {
    /**
     * Handle a Sessions command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        final String optionString = getFullParam(user, params, 2, Arrays.asList("all", "authenticated", "unauthenticated", "account", ""));
        if (optionString == null) { return; }

        if (user.getAccount().isAdmin() && !optionString.equalsIgnoreCase("")) {
            user.sendBotMessage("Currently connected sockets (Type: %s):", optionString);
            user.sendBotMessage("");
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
                sb.append("     (");
                sb.append(u.toString());
                sb.append(")");

                user.sendBotMessage(sb.toString());
                matched++;
            }
            user.sendBotMessage("----------");
            user.sendBotMessage("Matched: %d    Total: %d", matched, count);
        } else {
            user.sendBotMessage("Currently connected sockets for this account:");
            user.sendBotMessage("");
            int count = 0;
            for (final UserSocket u : user.getAccount().getUserSockets()) {
                count++;
                user.sendBotMessage(u.getIP());
            }
            user.sendBotMessage("----------");
            user.sendBotMessage("Total: %d", count);
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

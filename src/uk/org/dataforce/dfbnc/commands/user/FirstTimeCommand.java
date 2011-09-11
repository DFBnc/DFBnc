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
 */
package uk.org.dataforce.dfbnc.commands.user;

import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.sockets.UserSocket;
import uk.org.dataforce.libs.util.Util;

/**
 * This file represents the 'FirstTime' command
 */
public class FirstTimeCommand extends Command {
    /**
     * Handle a FirstTime command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        if (params.length > 1 && params[1].equalsIgnoreCase("admin")) {
            if (user.getAccount().isAdmin()) {
                user.sendBotMessage("----------------");
                user.sendBotMessage("As an admin of this BNC you have addional commands available to you");
                user.sendBotMessage("You can see what these are by using:");
                user.sendBotMessage("    /dfbnc ShowCommands admin");
                user.sendBotMessage(" or");
                user.sendBotMessage("    /dfbnc ShowCommands all");
                user.sendBotMessage("----");
                user.sendBotMessage("You may view this again at anytime by issuing: /dfbnc firsttime admin");
            } else {
                user.sendBotMessage("You do not have permission to access this command.");
            }
        } else {
            user.sendBotMessage("----------------");
            user.sendBotMessage("Welcome to DFBnc. It seems that this is your first time using this account.");
            user.sendBotMessage("With DFBnc you can issue commands to the BNC using /dfbnc (or /raw dfbnc or /quote dfbnc depending on your client)");
            user.sendBotMessage("For example to see what commands are available you would use:");
            user.sendBotMessage("    /dfbnc ShowCommands");
            user.sendBotMessage("Some commands are only available when the corresponding servertype is in use, and as such will not be displayed if not available.");
            user.sendBotMessage("----");
            user.sendBotMessage("The first thing you will want to do is setup a server connection type, this can be done using:");
            user.sendBotMessage("    /dfbnc <servertype|st> settype <type>");
            user.sendBotMessage("The available types can be found out by omiting the <type> parameter.");
            user.sendBotMessage("Each account can only use one servertype at a time, however changing the servertype will not remove the settings associated with the old type");
            user.sendBotMessage("----");
            user.sendBotMessage("Once you have set the servertype the command:");
            user.sendBotMessage("    /dfbnc <servertype|st> help");
            user.sendBotMessage("Will give you more specific information about the servertype currently in use.");
            user.sendBotMessage("----");
                        user.sendBotMessage("Most server types will provide a 'serverlist' command you can use to add a server to connect to:");
                        user.sendBotMessage("    /dfbnc serverlist add irc.quakenet.org:6667");
                        user.sendBotMessage("----");
            user.sendBotMessage("Once you have your servertype setup correctly and servers added, you can use");
            user.sendBotMessage("    /dfbnc connect");
            user.sendBotMessage("To connect the bnc to the server");
            user.sendBotMessage("----");
            user.sendBotMessage("You may view this again at anytime by issuing: /dfbnc firsttime");
            user.sendBotMessage("----");
            user.sendBotMessage("In all cases /dfbnc can be substituted for /msg "+Util.getBotName()+". Example:");
            user.sendBotMessage("    /msg "+Util.getBotName()+" ShowCommands");
            user.sendBotMessage(" will do the same as:");
            user.sendBotMessage("    /dfbnc ShowCommands");
        }
        user.getAccount().setFirst(false);
    }
    
    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"firsttime", "*ft"};
    }
    
    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public FirstTimeCommand (final CommandManager manager) { super(manager); }
    
    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command gives information on firsttime usage of dfbnc";
    } 
}

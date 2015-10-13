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
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.util.Util;

import java.util.Arrays;

/**
 * This file represents the 'FirstTime' command
 */
public class FirstTimeCommand extends Command {
    /**
     * Handle a FirstTime command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        final String option = getFullParam(output, params, 2, Arrays.asList("admin", "user", ""));
        if (option == null) { return; }

        if (params.length > 2 && option.equalsIgnoreCase("admin")) {
            if (user.getAccount().isAdmin()) {
                output.addBotMessage("As an admin of this BNC you have addional commands available to you");
                output.addBotMessage("You can see what these are by using:");
                output.addBotMessage("    /dfbnc show commands admin");
                output.addBotMessage(" or");
                output.addBotMessage("    /dfbnc show commands all");
                output.addBotMessage("----");
                output.addBotMessage("You may view this again at anytime by issuing: /dfbnc show firsttime admin");
            } else {
                output.addBotMessage("You do not have permission to access this command.");
            }
        } else {
            output.addBotMessage("Welcome to DFBnc. It seems that this is your first time using this account.");
            output.addBotMessage("With DFBnc you can issue commands to the BNC using /dfbnc (or /raw dfbnc or /quote dfbnc depending on your client)");
            output.addBotMessage("For example to see what commands are available you would use:");
            output.addBotMessage("    /dfbnc show commands");
            output.addBotMessage("Some commands are only available when the corresponding servertype is in use, and as such will not be displayed if not available.");
            output.addBotMessage("----");
            output.addBotMessage("The first thing you will want to do is setup a server connection type, this can be done using:");
            output.addBotMessage("    /dfbnc servertype settype <type>");
            output.addBotMessage("The available types can be found out by omiting the <type> parameter.");
            output.addBotMessage("Each account can only use one servertype at a time, however changing the servertype will not remove the settings associated with the old type");
            output.addBotMessage("----");
            output.addBotMessage("Once you have set the servertype the command:");
            output.addBotMessage("    /dfbnc servertype help");
            output.addBotMessage("Will give you more specific information about the servertype currently in use.");
            output.addBotMessage("----");
            output.addBotMessage("Most server types will provide a 'serverlist' command you can use to add a server to connect to:");
            output.addBotMessage("    /dfbnc serverlist add irc.quakenet.org:6667");
            output.addBotMessage("----");
            output.addBotMessage("Once you have your servertype setup correctly and servers added, you can use");
            output.addBotMessage("    /dfbnc connect");
            output.addBotMessage("To connect the bnc to the server");
            output.addBotMessage("----");
            output.addBotMessage("You may view this again at anytime by issuing: /dfbnc show firsttime");
            output.addBotMessage("----");
            output.addBotMessage("In all cases /dfbnc can be substituted for /msg %s. Example:", Util.getBotName());
            output.addBotMessage("    /msg %s show commands", Util.getBotName());
            output.addBotMessage(" will do the same as:");
            output.addBotMessage("    /dfbnc show commands");
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
        return new String[]{"firsttime"};
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

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

import com.dmdirc.util.DateUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.dfbnc.DFBnc;
import com.dfbnc.DFBncDaemon;
import com.dfbnc.commands.AdminCommand;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutput;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.util.Util;
import uk.org.dataforce.libs.cliparser.CLIParam;
import uk.org.dataforce.libs.cliparser.CLIParser;

/**
 * This file represents the 'system' command
 */
public class SystemCommand extends AdminCommand {
    /**
     * Handle a system command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutput where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutput output) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        output.sendBotMessage("----------------------------------------");
        output.sendBotMessage("DFBnc System nformation");
        output.sendBotMessage("----------------------------------------");
        output.sendBotMessage("Started At: " + sdf.format(new Date(DFBnc.startTime)));
        final long upSeconds = (System.currentTimeMillis() - DFBnc.startTime) / 1000;
        output.sendBotMessage("Uptime: " + DateUtils.formatDuration((int)upSeconds));
        output.sendBotMessage("----------------------------------------");
        output.sendBotMessage("Startup Information:");
        output.sendBotMessage("--------------------");
        output.sendBotMessage("Forking Supported: " + (DFBncDaemon.canFork() ? "Yes" : "No"));
        if (DFBncDaemon.canFork()) {
            output.sendBotMessage("Forked: " + (DFBnc.daemon.isDaemonized() ? "Yes" : "No"));
            output.sendBotMessage("Current PID: " + DFBncDaemon.getPID());
            final List<String> var = DFBncDaemon.getArgs();
            output.sendBotMessage("Run Args: " + Util.joinString(var.toArray(new String[var.size()]), " ", 0, 0));
        }

        final CLIParser cli = CLIParser.getCLIParser();
        output.sendBotMessage("CLI Parser Args: " + Util.joinString(cli.getLastArgs(), " ", 0, 0));
        output.sendBotMessage("CLI Params:");
        for (final CLIParam p : cli.getParamList()) {
            output.sendBotMessage("    " + (p.getChr() != 0 ? p.getChr() : "") + "/" + p.getString() + " - " + p.getNumber() + " = " + p.getStringValue());
        }
        output.sendBotMessage("Redundant:");
        for (final String s : cli.getRedundant()) {
            output.sendBotMessage("    " + s);
        }

        output.sendBotMessage("----------------------------------------");
        output.sendBotMessage("Host Information:");
        output.sendBotMessage("--------------------");
        for (Entry<Object, Object> e : System.getProperties().entrySet()) {
            output.sendBotMessage("    %s: %s", e.getKey(), e.getValue());
        }
        output.sendBotMessage("----------------------------------------");
        output.sendBotMessage("Component Versions:");
        output.sendBotMessage("--------------------");
        for (Entry<String, String> e : DFBnc.getVersions().entrySet()) {
            output.sendBotMessage("    %s: %s", e.getKey(), e.getValue());
        }
        output.sendBotMessage("----------------------------------------");
        output.sendBotMessage("Sessions:");
        output.sendBotMessage("--------------------");
        int count = 0;
        for (final UserSocket u : UserSocket.getUserSockets()) {
            count++;
            final StringBuilder sb = new StringBuilder();

            sb.append(u.getInfo());
            sb.append(" - ");
            sb.append(u.getSocketID());
            sb.append(" - ");
            if (u.getAccount() == null) {
                sb.append("UNAUTHENTICATED");
            } else {
                sb.append(u.getAccount().getName());
                if (u.getAccount().isAdmin()) { sb.append('*'); }
            }
            sb.append("     (");
            sb.append(u.toString());
            sb.append(")");
            output.sendBotMessage(sb.toString());
        }
        output.sendBotMessage("      (%d current sessions)", count);
        output.sendBotMessage("----------------------------------------");

    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"system"};
    }

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public SystemCommand (final CommandManager manager) { super(manager); }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command tells you information about the system dfbnc is running on";
    }
}
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
package com.dfbnc.commands.show;

import com.dfbnc.DFBnc;
import com.dfbnc.DFBncDaemon;
import com.dfbnc.commands.AdminCommand;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.util.Util;
import com.dmdirc.util.DateUtils;
import uk.org.dataforce.libs.cliparser.CLIParam;
import uk.org.dataforce.libs.cliparser.CLIParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

/**
 * This file represents the 'system' command
 */
public class SystemCommand extends AdminCommand {
    /**
     * Handle a system command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        output.addBotMessage("----------------------------------------");
        output.addBotMessage("DFBnc System Information");
        output.addBotMessage("----------------------------------------");
        output.addBotMessage("Started At: %s", sdf.format(new Date(DFBnc.startTime)));
        final long upSeconds = (System.currentTimeMillis() - DFBnc.startTime) / 1000;
        output.addBotMessage("Uptime: %s", DateUtils.formatDuration((int) upSeconds));
        output.addBotMessage("----------------------------------------");
        output.addBotMessage("Startup Information:");
        output.addBotMessage("--------------------");
        output.addBotMessage("Forking Supported: %s", (DFBncDaemon.canFork() ? "Yes" : "No"));
        if (DFBncDaemon.canFork()) {
            output.addBotMessage("Forked: %s", (DFBnc.daemon.isDaemonized() ? "Yes" : "No"));
            output.addBotMessage("Current PID: %s", DFBncDaemon.getPID());
            final List<String> var = DFBncDaemon.getArgs();
            output.addBotMessage("Run Args: %s", Util.joinString(var.toArray(new String[var.size()]), " ", 0, 0));
        }

        final CLIParser cli = CLIParser.getCLIParser();
        output.addBotMessage("CLI Parser Args: %s", Util.joinString(cli.getLastArgs(), " ", 0, 0));
        output.addBotMessage("CLI Params:");
        for (final CLIParam p : cli.getParamList()) {
            output.addBotMessage("    %s", (p.getChr() != 0 ? p.getChr() : "") + "/" + p.getString() + " - " + p.getNumber() + " = " + p.getStringValue());
        }
        output.addBotMessage("Redundant:");
        for (final String s : cli.getRedundant()) {
            output.addBotMessage("    %s", s);
        }

        output.addBotMessage("----------------------------------------");
        output.addBotMessage("Host Information:");
        output.addBotMessage("--------------------");
        for (Entry<Object, Object> e : System.getProperties().entrySet()) {
            output.addBotMessage("    %s: %s", e.getKey(), e.getValue());
        }
        output.addBotMessage("----------------------------------------");
        output.addBotMessage("Component Versions:");
        output.addBotMessage("--------------------");
        myManager.getCommand("version").ifPresent(c -> c.handle(user, new String[]{"version", "all"}, output));
        output.addBotMessage("----------------------------------------");
        output.addBotMessage("Connections:");
        output.addBotMessage("--------------------");
        myManager.getCommand("connections").ifPresent(c -> c.handle(user, new String[]{"connections", "full", "all"}, output));
        output.addBotMessage("----------------------------------------");
        output.addBotMessage("Logging:");
        output.addBotMessage("--------------------");
        myManager.getCommand("logging").ifPresent(c -> c.handle(user, new String[]{"connections", "full", "all"}, output));
        output.addBotMessage("----------------------------------------");

    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"system", "*tech-support"};
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

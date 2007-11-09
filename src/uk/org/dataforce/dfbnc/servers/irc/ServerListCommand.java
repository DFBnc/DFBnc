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
package uk.org.dataforce.dfbnc.servers.irc;

import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.UserSocket;
import uk.org.dataforce.dfbnc.DFBnc;

import java.util.ArrayList;
import java.util.List;

/**
 * This file represents the 'ServerList' command
 */
public class ServerListCommand extends Command {
	/**
	 * Handle a ServerList command.
	 *
	 * @param user the UserSocket that performed this command
	 * @param params Params for command (param 0 is the command name)
	 */
	public void handle(final UserSocket user, final String[] params) {
		user.sendBotMessage("----------------");
		if (params.length > 1) {
			List<String> serverList = new ArrayList<String>();
			serverList = user.getAccount().getProperties().getListProperty("irc.serverlist", serverList);
			if (params[1].equalsIgnoreCase("list")) {
				if (serverList.size() > 0) {
					user.sendBotMessage("You currently have the following servers in your list:");
					for (int i = 0; i < serverList.size(); ++i) {
						user.sendBotMessage(String.format("    %2d: %s", i, serverList.get(i)));
					}
				} else {
					user.sendBotMessage("Your server list is currently empty.");
				}
			} else if (params[1].equalsIgnoreCase("add")) {
				if (params.length > 2) {
					StringBuilder allInput = new StringBuilder("");
					for (int i = 2 ; i < params.length; ++i) { allInput.append(params[i]+" "); }
					String[] input = IRCServerType.parseServerString(allInput.toString());
					serverList.add(input[3]);
					user.sendBotMessage("Server ("+input[3]+") has been added to your serverList");
				} else {
					user.sendBotMessage("You must specify a server to add in the format: <server>[:port] [password]");
				}
			} else if (params[1].equalsIgnoreCase("del")) {
				if (params.length > 2) {
					try {
						final int position = Integer.parseInt(params[2]);
						if (position < serverList.size()) {
							final String serverName = serverList.get(position);
							serverList.remove(position);
							user.sendBotMessage("Server number "+position+" ("+serverName+") has been removed from your server list.");
						} else {
							user.sendBotMessage("There is no server with the number "+position+" in your server list");
							user.sendBotMessage("Use /dfbnc "+params[0]+" list to view your server list");
						}
					} catch (NumberFormatException nfe) {
						user.sendBotMessage("You must specify a server number to delete");
					}
				} else {
					user.sendBotMessage("You must specify a server number to delete");
				}
			} else if (params[1].equalsIgnoreCase("clear")) {
				serverList.clear();
				user.sendBotMessage("Your server list has been cleared.");
			}
			user.getAccount().getProperties().setListProperty("irc.serverlist", serverList);
		} else {
			user.sendBotMessage("This command can be used to modify your irc serverlist using the following params:");
			user.sendBotMessage("  /dfbnc "+params[0]+" list");
			user.sendBotMessage("  /dfbnc "+params[0]+" add <Server>[:Port] [password]");
			user.sendBotMessage("  /dfbnc "+params[0]+" del <number>");
			user.sendBotMessage("  /dfbnc "+params[0]+" clear");
		}
	}
	
	/**
	 * What does this Command handle.
	 *
	 * @return String[] with the names of the tokens we handle.
	 */
	public String[] handles() {
		return new String[]{"serverlist", "sl"};
	}
	
	/**
	 * Create a new instance of the Command Object
	 *
	 * @param manager CommandManager that is in charge of this Command
	 */
	public ServerListCommand (final CommandManager manager) { super(manager); }
	
	/**
	 * Get a description of what this command does
	 *
	 * @return A description of what this command does
	 */
	public String getDescription() {
		return "This command lets you manipulate the irc server list";
	}
	
	/**
	 * Get SVN ServerList information.
	 *
	 * @return SVN ServerList String
	 */
	public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }	
}
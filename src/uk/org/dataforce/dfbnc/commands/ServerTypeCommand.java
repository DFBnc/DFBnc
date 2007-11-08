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
package uk.org.dataforce.dfbnc.commands;

import uk.org.dataforce.dfbnc.UserSocket;
import uk.org.dataforce.dfbnc.Functions;
import uk.org.dataforce.dfbnc.DFBnc;
import uk.org.dataforce.dfbnc.servers.ServerType;

/**
 * This file represents the 'ServerType' command
 */
public class ServerTypeCommand extends Command {
	/**
	 * Handle a ServerType command.
	 *
	 * @param user the UserSocket that performed this command
	 * @param params Params for command (param 0 is the command name)
	 */
	public void handle(final UserSocket user, final String[] params) {
		if (params.length > 1 && params[1].equalsIgnoreCase("settype")) {
			user.sendBotMessage("----------------");
			if (params.length > 2) {
				try {
					ServerType serverType = DFBnc.getServerTypeManager().getServerType(params[2]);
					serverType.activate(user.getAccount());
					user.getAccount().getProperties().setProperty("servertype", params[2].toLowerCase());
					user.sendBotMessage("Your ServerType is now "+params[2].toLowerCase()+".");
				} catch (Exception e) {
					user.sendBotMessage("Sorry, "+params[2]+" is not a valid ServerType");
				}
			} else {
				user.sendBotMessage("Available Types:");
				for (String server : DFBnc.getServerTypeManager().getServerTypeNames()) {
					try {
						ServerType serverType = DFBnc.getServerTypeManager().getServerType(server);
						user.sendBotMessage("    "+server+" - "+serverType.getDescription());
					} catch (Exception e) { /* Should never happen */}
				}
			}
		} else if (params.length > 1 && params[1].equalsIgnoreCase("help")) {
			user.sendBotMessage("----------------");
			user.sendBotMessage("This command allows you to set the servertype for this account.");
			final String currentType = user.getAccount().getProperties().getProperty("servertype", "");
			if (currentType.equals("")) {
				user.sendBotMessage("You currently do not have a servertype selected.");
			} else {
				user.sendBotMessage("Your current servertype is: "+currentType);
			}
			user.sendBotMessage("");
			user.sendBotMessage("You can set your type using the command: /dfbnc "+params[0]+" settype <type>");
			user.sendBotMessage("A list of available types can be seen by ommiting the <type> param");
		} else {
			user.sendBotMessage("For usage information use /dfbnc "+params[0]+" help");
		}
	}
	
	/**
	 * What does this Command handle.
	 *
	 * @return String[] with the names of the tokens we handle.
	 */
	public String[] handles() {
		return new String[]{"servertype", "st"};
	}
	
	/**
	 * Create a new instance of the Command Object
	 *
	 * @param manager CommandManager that is in charge of this Command
	 */
	protected ServerTypeCommand (final CommandManager manager) { super(manager); }
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }	
}
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

import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.UserSocket;

import java.util.TreeMap;
import java.util.SortedMap;
import java.util.ArrayList;

/**
 * This file represents the 'ShowCommands' command
 */
public class ShowCommandsCommand extends Command {
	/**
	 * Handle a ShowCommands command.
	 *
	 * @param user the UserSocket that performed this command
	 * @param params Params for command (param 0 is the command name)
	 */
	@Override
	public void handle(final UserSocket user, final String[] params) {
		// This stores the output for any admin commands we run across, these are
		// displayed at the end after the normal-user commands.
		ArrayList<String> adminCommands = new ArrayList<String>();
	
		user.sendBotMessage("----------------");
		user.sendBotMessage("The following commands are available to you:");
		CommandManager cmdmgr = user.getAccount().getCommandManager();
		SortedMap<String, Command> commands = new TreeMap<String, Command>(cmdmgr.getAllCommands());
		for (String commandName : commands.keySet()) {
			final Command command = commands.get(commandName);
			if (command.isAdminOnly()) {
				adminCommands.add(String.format("%-20s - %s", commandName, command.getDescription(commandName)));
			} else {
				user.sendBotMessage(String.format("%-20s - %s", commandName, command.getDescription(commandName)));
			}
		}
		
		if (user.getAccount().isAdmin()) {
			if (adminCommands.size() > 0) {
				user.sendBotMessage("");
				user.sendBotMessage("----------------");
				user.sendBotMessage("The following admin-only commands are also available to you:");
				for (String output : adminCommands) {
					user.sendBotMessage(output);
				}
			}
		}
	}
	
	/**
	 * What does this Command handle.
	 *
	 * @return String[] with the names of the tokens we handle.
	 */
	@Override
	public String[] handles() {
		return new String[]{"showcommands"};
	}
	
	/**
	 * Get a description of what this command does
	 *
	 * @param command The command to describe (incase one Command does multiple
	 *                things under different names)
	 * @return A description of what this command does
	 */
	@Override
	public String getDescription(final String command) {
		return "This command shows what commands are available to you";
	}
	
	/**
	 * Create a new instance of the Command Object
	 *
	 * @param manager CommandManager that is in charge of this Command
	 */
	public ShowCommandsCommand (final CommandManager manager) { super(manager); }
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }	
}
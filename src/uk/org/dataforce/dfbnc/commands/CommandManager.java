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

import uk.org.dataforce.logger.Logger;
import uk.org.dataforce.dfbnc.UserSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DFBNC Command Manager.
 * Manages adding/removing/calling commands.
 *
 * @author Shane Mc Cormack
 * @version $Id: CommandManager.java 1360 2007-05-25 19:12:05Z ShaneMcC $
 */
public final class CommandManager {
	/** HashMap used to store the different types of Command known. */
	private HashMap<String,Command> knownCommands = new HashMap<String,Command>();
	
	/** List used to store sub command mamangers */
	private List<CommandManager> subManagers = new ArrayList<CommandManager>();
	
	/** Nesting limit for calls to getCommand() */
	private final static int nestingLimit = 10;

	/**
	 * Constructor to create a CommandManager
	 */
	public CommandManager() { }
	
	/**
	 * Constructor to create a CommandManager, specifying a sub command manager.
	 *
	 * @param submanager Sub command manager to add
	 */
	public CommandManager(final CommandManager submanager) {
		subManagers.add(submanager);
	}
	
	/**
	 * Remove all commands
	 */
	public void empty() {
		knownCommands.clear();
	}
	
	/** Empty clone method to prevent cloning to get more copies of the CommandManager */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * This checks if this CommandManager, or any of its sub-managers has the
	 * given manager.
	 *
	 * @param manager CommandManager to look for.
	 */
	public boolean hasSubCommandManager(final CommandManager manager) {	
		if (subManagers.contains(manager)) {
			return true;
		} else {
			for (CommandManager subManager : subManagers) {
				if (subManager.hasSubCommandManager(manager)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Get all the commands available to this CommandManager.
	 * This is a *very* expensive command.
	 *
	 * @return Map of available commands.
	 */
	public Map<String, Command> getAllCommands() {	
		// First get our own commands,
		Map<String, Command> result = new HashMap<String, Command>(knownCommands);
		
		// Now all our submanagers commands
		for (CommandManager subManager : subManagers) {
			Map<String, Command> subResult = subManager.getAllCommands();
			for (String commandName : subResult.keySet()) {
				if (!result.containsKey(commandName)) {
					result.put(commandName, subResult.get(commandName));
				}
			}
		}
		
		return result;
	}

	/**
	 * Add new Sub Command Manager.
	 *
	 * @param manager Sub CommandManager to add.
	 * @return true if the CommandManager was added, else false.
	 */
	public boolean addSubCommandManager(final CommandManager manager) {	
		// Check that we don't have this already, its not us, and it doesn't have us.
		if (!hasSubCommandManager(manager) && manager != this && !manager.hasSubCommandManager(this)) {
			// now check that this doesn't have any of our sub-managers available
			for (CommandManager subManager : subManagers) {
				if (manager.hasSubCommandManager(subManager)) {
					return false;
				}
			}
			subManagers.add(manager);
			return true;
		}
		return false;
	}
	
	/**
	 * Delete Sub Command Manager.
	 *
	 * @param manager Sub CommandManager to remove.
	 * @return true if the CommandManager was removed, else false.
	 */
	public boolean delSubCommandManager(final CommandManager manager) {	
		if (subManagers.contains(manager)) {
			subManagers.remove(manager);
			return true;
		}
		return false;
	}

	/**
	 * Add new Command.
	 *
	 * @param command Command subclass for the command.
	 */
	public void addCommand(final Command command) {	
		// handles() returns a String array of all the namess
		// that this command will handle.
		addCommand(command.handles(), command);
	}
	
	/**
	 * Add a command using given handles for the command
	 *
	 * @param handles String Array of tokens to add this command as a hadler for
	 * @param command Command subclass for the Command.
	 * @return the Command that was added, or null if adding failed.
	 */
	public Command addCommand(final String[] handles, final Command command) {	
		Logger.debug("Adding command: "+command.getName());
		
		try {
			for (int i = 0; i < handles.length; ++i) {
				if (knownCommands.containsKey(handles[i].toLowerCase())) {
					// New Commands take priority over old ones
					knownCommands.remove(handles[i].toLowerCase());
				}
				Logger.debug2("\t Added handler for: "+handles[i]);
				knownCommands.put(handles[i].toLowerCase(), command);
			}
		} catch (Exception e) {
			Logger.error("Error adding Command '"+command.toString()+"': "+e.getMessage());
			delCommand(command);
			return null;
		}
		return command;
	}
		
	/**
	 * Remove a Command type.
	 *
	 * @param command Command subclass for the command.
	 */
	public void delCommand(final Command command) {	
		Command testCommand;
		Logger.debug("Deleting command: "+command.getName());
		for (String elementName : knownCommands.keySet()) {
			Logger.debug2("\t Checking handler for: "+elementName);
			testCommand = knownCommands.get(elementName);
			if (testCommand.getName().equalsIgnoreCase(command.getName())) {
				Logger.debug2("\t Removed handler for: "+elementName);
				knownCommands.remove(elementName);
			}
		}
	}
	
	/**
	 * Get the command used for a specified name.
	 *
	 * @param name Name to look for
	 * @return Command for the given name.
	 */
	public Command getCommand(final String name) throws CommandNotFound {
		return getCommand(name, 0);
	}
	
	/**
	 * Get the command used for a specified name.
	 *
	 * @param name Name to look for
	 * @param nesting Amount of previous calls.
	 * @return Command for the given name.
	 */
	protected Command getCommand(final String name, final int nesting) throws CommandNotFound {
		if (knownCommands.containsKey(name.toLowerCase())) {
			return knownCommands.get(name.toLowerCase());
		} else {
			if (nesting <= nestingLimit) {
				for (CommandManager manager : subManagers) {
					try {
						return manager.getCommand(name, (nesting+1));
					} catch (CommandNotFound cnf) { /* Ignore, it might be in other managers */ }
				}
			}
			// Command was not found in any manager.
			throw new CommandNotFound("No command is known by "+name);
		}
	}
	
	/**
	 * Handle a command.
	 *
	 * @param user UserSocket that issued the command
	 * @param params Params for command (param0 is the command name)
	 * @throws CommandNotFound exception if no commands exists to handle the line
	 */
	public void handle(final UserSocket user, final String[] params) throws CommandNotFound {
		Command commandHandler = null;
		try {
			commandHandler = getCommand(params[0]);
			if (commandHandler.isAdminOnly() && !user.getAccount().isAdmin()) {
				throw new CommandNotFound("No command is known by "+params[0]);
			} else {
				commandHandler.handle(user, params);
			}
		} catch (CommandNotFound p) {
			throw p;
		} catch (Exception e) {
//			StringBuilder line = new StringBuilder();
//			for (int i = 0; i < token.length; ++i ) { line.append(" ").append(token[i]); }
			Logger.error("There has been an error with the command '"+params[0]+"'");
		}
	}
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id: CommandManager.java 1360 2007-05-25 19:12:05Z ShaneMcC $"; }	
}


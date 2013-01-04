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
package uk.org.dataforce.dfbnc.commands;

import java.util.AbstractMap.SimpleImmutableEntry;
import uk.org.dataforce.libs.logger.Logger;
import uk.org.dataforce.dfbnc.sockets.UserSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import uk.org.dataforce.dfbnc.DFBnc;

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

    /**
     * Empty clone method to prevent cloning to get more copies of the CommandManager
     *
     * @throws CloneNotSupportedException Always
     * @return Nothing
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * This checks if this CommandManager, or any of its sub-managers has the
     * given manager.
     *
     * @param manager CommandManager to look for.
     * @return True if manager is a SubManager of this or one of its SubManagers.
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
     * This is potentially expensive.
     *
     * @param allowAdmin Allow admin-only commands?
     * @return Map of available commands.
     */
    public Map<String, Command> getAllCommands(final boolean allowAdmin) {
        return getAllCommands("", allowAdmin);
    }

    /**
     * Get all the commands available to this CommandManager.
     * This is potentially expensive.
     *
     * @param startsWith Limit to commands that start with this string. "" or
     *        "?" will return all commands.
     * @param allowAdmin Allow admin-only commands?
     * @return Map of available commands.
     */
    public Map<String, Command> getAllCommands(final String startsWith, final boolean allowAdmin) {
        // First get our own commands,
        Map<String, Command> result;
        final String sw = startsWith.toLowerCase();

        if (startsWith.isEmpty() || startsWith.equals("?")) {
            if (allowAdmin) {
                result = new HashMap<String, Command>(knownCommands);
            } else {
                // For non-admins we actually need to check all the commands to
                // see if we are allowed use it or not.
                result = new HashMap<String, Command>();
                for (Entry<String, Command> entry : new HashMap<String, Command>(knownCommands).entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            result = new HashMap<String, Command>();
            for (Entry<String, Command> entry : new HashMap<String, Command>(knownCommands).entrySet()) {
                if (!result.containsKey(entry.getKey()) && (entry.getKey().startsWith(sw) || entry.getKey().startsWith("*" + sw)) && (allowAdmin || !entry.getValue().isAdminOnly())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Now all our submanagers commands
        for (CommandManager subManager : subManagers) {
            Map<String, Command> subResult = subManager.getAllCommands(startsWith, allowAdmin);
            for (Entry<String, Command> entry : subResult.entrySet()) {
                if (!result.containsKey(entry.getKey())) {
                    result.put(entry.getKey(), entry.getValue());
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
        // handles() returns a String array of all the main names that
        // this command will handle
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
            for (String handle : handles) {
                if (knownCommands.containsKey(handle.toLowerCase())) {
                    // New Commands take priority over old ones
                    knownCommands.remove(handle.toLowerCase());
                }
                Logger.debug2("\t Added handler for: "+handle);
                knownCommands.put(handle.toLowerCase(), command);
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
     * Get the matching command used for a specified name.
     * If AllowShortCommands is off, this returns the same as getCommand would,
     * otherwise this tries to find a single matching command. If multiple
     * matching commands are found, but only one of them is non-hidden, it will
     * be returned.
     *
     * @param name Name to look for
     * @param allowAdmin Allow admin-only commands?
     * @return Command Entry for the given name.f
     * @throws CommandNotFoundException If the requested command doesn't exist in this or any sub managers
     */
    public Entry<String, Command> getMatchingCommand(final String name, final boolean allowAdmin) throws CommandNotFoundException {
        CommandNotFoundException cnfe = null;

        Logger.debug5("Looking for matching command: " + name);

        try {
            return new SimpleImmutableEntry<String, Command>(name, getCommand(name, allowAdmin));
        } catch (CommandNotFoundException p) {
            cnfe = p;
        }

        Logger.debug5("No exact match found.");

        if (DFBnc.getBNC().getConfig().getBoolOption("general", "allowshortcommands", true)) {
            Logger.debug5("Short commands enabled.");
            // Find a matching command.
            final Map<String, Command> cmds = new TreeMap<String, Command>(getAllCommands(name, allowAdmin));
            final Set<Command> commands = new HashSet<Command>(cmds.values());
            Logger.debug5("Matching Handlers: " + cmds.size());
            Logger.debug5("Matching Commands: " + commands.size());

            // Check for only 1 resulting command.
            // This checks for only one handler for the given word, or in the
            // case of multiple matching handlers, are they actually just the
            // same command anyway?
            if (cmds.size() == 1 || commands.size() == 1) {
                for (Entry<String, Command> entry : cmds.entrySet()) {
                    Logger.debug5("Matching Handler: " + entry);
                    // Don't match this command if the short form is not permitted.
                    if (!entry.getValue().allowShort(entry.getKey())) {
                        throw cnfe;
                    }

                    String handlerName = entry.getKey().charAt(0) == '*' ? entry.getKey().substring(1) : entry.getKey();
                    if (cmds.size() > 1) {
                        // Single command, but multiple handles. Use the
                        // earliest one from the handles array.
                        Logger.debug5("Multi handler match");
                        for (String handle : entry.getValue().handles()) {
                            if (handle.toLowerCase().startsWith(name.toLowerCase())) {
                                handlerName = handle;
                                break;
                            }
                        }
                    }
                    return new SimpleImmutableEntry<String, Command>(handlerName, entry.getValue());
                }
            } else {
                // Last ditch attempt, see if there is a single non-hidden
                // command returned.
                Entry<String, Command> unhidden = null;
                for (Entry<String, Command> entry : cmds.entrySet()) {
                    // Check if this is a non-hidden entry
                    if (entry.getKey().charAt(0) != '*') {
                        // If we have found no un-hidden entries yet, save it
                        // otherwise, if we have already found one, then abort
                        // and forget about any we found.
                        if (unhidden == null) {
                            unhidden = new SimpleImmutableEntry<String, Command>(entry);
                        } else {
                            unhidden = null;
                            break;
                        }
                    }
                }

                if (unhidden == null) {
                    throw cnfe;
                } else {
                    return unhidden;
                }
            }
        }

        throw cnfe;
    }

    /**
     * Get the command used for a specified name.
     *
     * @param name Name to look for
     * @return Command for the given name.
     * @throws CommandNotFoundException If the requested command doesn't exist in this or any sub managers
     */
    public Command getCommand(final String name) throws CommandNotFoundException {
        return getCommand(name, true, 0);
    }

    /**
     * Get the command used for a specified name.
     *
     * @param name Name to look for
     * @param allowAdmin Allow admin-only commands?
     * @return Command for the given name.
     * @throws CommandNotFoundException If the requested command doesn't exist in this or any sub managers
     */
    public Command getCommand(final String name, final boolean allowAdmin) throws CommandNotFoundException {
        return getCommand(name, allowAdmin, 0);
    }

    /**
     * Get the command used for a specified name.
     *
     * @param name Name to look for
     * @param allowAdmin Allow admin-only commands?
     * @param nesting Amount of previous calls.
     * @return Command for the given name.
     * @throws CommandNotFoundException If the requested command doesn't exist in this or any sub managers
     */
    protected Command getCommand(final String name, final boolean allowAdmin, final int nesting) throws CommandNotFoundException {
        Command result = null;
        if (knownCommands.containsKey(name.toLowerCase())) {
            result = knownCommands.get(name.toLowerCase());
        } else if (knownCommands.containsKey("*"+name.toLowerCase())) {
            result = knownCommands.get("*"+name.toLowerCase());
        }

        if (result == null || (result.isAdminOnly() && !allowAdmin)) {
            if (nesting <= nestingLimit) {
                for (CommandManager manager : subManagers) {
                    try {
                        return manager.getCommand(name, allowAdmin, (nesting+1));
                    } catch (CommandNotFoundException cnf) { /* Ignore, it might be in other managers */ }
                }
            }
            // Command was not found in any manager.
            // If short commands are enabled, try to find a matching command.

            throw new CommandNotFoundException("No command is known by "+name);
        } else {
            return result;
        }
    }

    /**
     * Handle a command.
     *
     * @param user UserSocket that issued the command
     * @param params Params for command (param0 is the command name)
     * @throws CommandNotFoundException exception if no commands exists to handle the line
     */
    public void handle(final UserSocket user, final String[] params) throws CommandNotFoundException {
        if (params.length == 0 || params[0] == null || params[0].isEmpty()) {
            throw new CommandNotFoundException("No valid command given.");
        }

        final Entry<String, Command> e = getMatchingCommand(params[0], user.getAccount().isAdmin());
        String[] handleParams = params;
        handleParams[0] = e.getKey();
        final Command commandHandler = e.getValue();

        try {
            if (commandHandler.isAdminOnly() && !user.getAccount().isAdmin()) {
                throw new CommandNotFoundException("No command is known by "+params[0]);
            } else {
                commandHandler.handle(user, handleParams);
            }
        } catch (Exception ex) {
            Logger.error("There has been an error with the command '"+params[0]+"'");
            ex.printStackTrace();
        }
    }
}


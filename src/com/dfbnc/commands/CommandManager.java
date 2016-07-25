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
package com.dfbnc.commands;

import com.dfbnc.DFBnc;
import com.dfbnc.config.Config;
import com.dfbnc.sockets.UserSocket;
import uk.org.dataforce.libs.logger.Logger;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * DFBNC Command Manager.
 * Manages adding/removing/calling commands.
 */
public final class CommandManager {

    /** Command name prefix used in {@link #knownCommands} for hidden commands (e.g. aliases). */
    private static final char HIDDEN_PREFIX = '*';

    /** Nesting limit for calls to getCommand() */
    private static final int NESTING_LIMIT = 10;

    /** HashMap used to store the different types of Command known. */
    private final Map<String, Command> knownCommands = new HashMap<>();

    /** List used to store sub command managers */
    private final List<CommandManager> subManagers = new ArrayList<>();

    /** Config to use to get settings. */
    private final Config config;

    /**
     * Constructor to create a CommandManager using the global config.
     */
    public CommandManager() { this(null); }

    /**
     * Constructor to create a CommandManager using a given config.
     *
     * @param config Config to use to get settings.
     */
    public CommandManager(final Config config) {
        this.config = config;
    }

    /**
     * Get the config to use.
     * This is because we need a config later, but DFBnc.getBNC() may not
     * work yet.
     *
     * @return The config to use, based on which constructor.
     */
    private Config getConfig() {
        return config == null ? DFBnc.getBNC().getConfig() : config;
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
        return subManagers.parallelStream().anyMatch(s -> s == manager || s.hasSubCommandManager(manager));
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
        final String sw = startsWith.toLowerCase();
        final boolean allCommands = startsWith.isEmpty() || startsWith.equals("?");

        // First get our own commands
        final Map<String, Command> result = knownCommands
                .entrySet()
                .parallelStream()
                .filter(e -> allowAdmin || !e.getValue().isAdminOnly())
                .filter(e -> allCommands || e.getKey().startsWith(sw) || e.getKey().startsWith(HIDDEN_PREFIX + sw))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        // Now all our submanagers' commands
        for (CommandManager subManager : subManagers) {
            result.putAll(
                    subManager.getAllCommands(startsWith, allowAdmin)
                            .entrySet()
                            .parallelStream()
                            .filter(entry -> !result.containsKey(entry.getKey()))
                            .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
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
        return !hasSubCommandManager(manager)
                && manager != this
                && !manager.hasSubCommandManager(this)
                && subManagers.parallelStream().noneMatch(manager::hasSubCommandManager)
                && subManagers.add(manager);
    }

    /**
     * Delete Sub Command Manager.
     *
     * @param manager Sub CommandManager to remove.
     * @return true if the CommandManager was removed, else false.
     */
    public boolean delSubCommandManager(final CommandManager manager) {
        return subManagers.remove(manager);
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
                Logger.debug2("\t Added handler for: "+handle);
                // New Commands take priority over old ones
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
        Logger.debug("Deleting command: " + command.getName());
        knownCommands.values().removeIf(c -> c.getName().equalsIgnoreCase(command.getName()));
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
     * @return Command Entry for the given name.
     */
    public Optional<Entry<String, Command>> getMatchingCommand(final String name, final boolean allowAdmin) {
        Logger.debug5("Looking for matching command: " + name);

        Optional<Command> command = getCommand(name, allowAdmin);
        if (command.isPresent()) {
            return command.map(c -> new SimpleImmutableEntry<>(name, c));
        }

        Logger.debug5("No exact match found.");

        if (getConfig().getOptionBool("general", "allowshortcommands")) {
            Logger.debug5("Short commands enabled.");
            // Find a matching command.
            final Map<String, Command> cmds = new TreeMap<>(getAllCommands(name, allowAdmin));
            final Set<Command> commands = new HashSet<>(cmds.values());
            Logger.debug5("Matching Handlers: " + cmds.size());
            Logger.debug5("Matching Commands: " + commands.size());

            // Check for only 1 resulting command.
            // This checks for only one handler for the given word, or in the
            // case of multiple matching handlers, are they actually just the
            // same command anyway?
            if (cmds.size() == 1 || commands.size() == 1) {
                final Entry<String, Command> entry = cmds.entrySet().iterator().next();
                Logger.debug5("Matching Handler: " + entry);
                // Don't match this command if the short form is not permitted.
                if (!entry.getValue().allowShort(entry.getKey())) {
                    return Optional.empty();
                }

                String handlerName = entry.getKey().charAt(0) == HIDDEN_PREFIX ? entry.getKey().substring(1) : entry.getKey();
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
                return Optional.of(new SimpleImmutableEntry<>(handlerName, entry.getValue()));
            } else {
                // Last ditch attempt, see if there is a single non-hidden
                // command returned.
                Entry<String, Command> unhidden = null;
                for (Entry<String, Command> entry : cmds.entrySet()) {
                    // Check if this is a non-hidden entry
                    if (entry.getKey().charAt(0) != HIDDEN_PREFIX) {
                        // If we have found no un-hidden entries yet, save it
                        // otherwise, if we have already found one, then abort
                        // and forget about any we found.
                        if (unhidden == null) {
                            unhidden = new SimpleImmutableEntry<>(entry);
                        } else {
                            unhidden = null;
                            break;
                        }
                    }
                }

                return Optional.ofNullable(unhidden);
            }
        }

        return Optional.empty();
    }

    /**
     * Get the command used for a specified name.
     *
     * @param name Name to look for
     * @return Command for the given name.
     */
    public Optional<Command> getCommand(final String name) {
        return getCommand(name, true, 0);
    }

    /**
     * Get the command used for a specified name.
     *
     * @param name Name to look for
     * @param allowAdmin Allow admin-only commands?
     * @return Command for the given name.
     */
    public Optional<Command> getCommand(final String name, final boolean allowAdmin) {
        return getCommand(name, allowAdmin, 0);
    }

    /**
     * Get the command used for a specified name.
     *
     * @param name Name to look for
     * @param allowAdmin Allow admin-only commands?
     * @param nesting Amount of previous calls.
     * @return Command for the given name.
     */
    protected Optional<Command> getCommand(final String name, final boolean allowAdmin, final int nesting) {
        Command result = null;
        if (knownCommands.containsKey(name.toLowerCase())) {
            result = knownCommands.get(name.toLowerCase());
        } else if (knownCommands.containsKey(HIDDEN_PREFIX + name.toLowerCase())) {
            result = knownCommands.get(HIDDEN_PREFIX + name.toLowerCase());
        }

        if (result != null && (!result.isAdminOnly() || allowAdmin)) {
            return Optional.of(result);
        }

        if (nesting <= NESTING_LIMIT) {
            return subManagers.stream()
                    .map(m -> m.getCommand(name, allowAdmin, nesting + 1))
                    .filter(Optional::isPresent)
                    .findFirst()
                    .orElse(Optional.<Command>empty());
        }

        return Optional.empty();
    }

    /**
     * Handle a command.
     *
     * @param user UserSocket that issued the command
     * @param params Params for command (param0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     * @throws CommandNotFoundException exception if no commands exists to handle the line
     * @throws CommandException exception if there was an exception running the command.
     */
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) throws CommandNotFoundException, CommandException {
        if (params.length == 0 || params[0] == null || params[0].isEmpty()) {
            throw new CommandNotFoundException("No valid command given.");
        }

        final Optional<Entry<String, Command>> e = getMatchingCommand(params[0], (user.getAccount().isAdmin() && !user.isReadOnly()));
        if (!e.isPresent()) {
            throw new CommandNotFoundException("Command not found: " + params[0]);
        }

        String[] handleParams = params;
        handleParams[0] = e.get().getKey();
        final Command commandHandler = e.get().getValue();

        try {
            if (commandHandler.isAdminOnly() && (!user.getAccount().isAdmin() || user.isReadOnly())) {
                throw new CommandNotFoundException("No command is known by "+params[0]);
            } else {
                commandHandler.handle(user, handleParams, output);
            }
        } catch (CommandNotFoundException cnfe) {
            throw cnfe;
        } catch (Exception ex) {
            Logger.error("There has been an error with the command '"+params[0]+"'");
            ex.printStackTrace();
            throw new CommandException(ex.getMessage(), ex);
        }
    }
}


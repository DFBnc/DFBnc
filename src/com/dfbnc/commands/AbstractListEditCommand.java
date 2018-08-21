/*
 * Copyright (c) 2006-2017 DFBnc Developers
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

import com.dfbnc.config.Config;
import com.dfbnc.sockets.UserSocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This file represents a listedit command.
 */
public abstract class AbstractListEditCommand extends Command {

    /**
     * Get the name of the property to store the list in.
     *
     * @param command The command passed as param[0]
     * @return The name of the property to store the list in.
     */
    public abstract String getPropertyName(final String command);

    /**
     * Get the name of the domain to store the list in.
     *
     * @param command The command passed as param[0]
     * @return The name of the domain to store the list in.
     */
    public abstract String getDomainName(final String command);

    /**
     * Get the name of the list.
     * This is used in various outputs from the command.
     *
     * @param command The command passed as param[0]
     * @return The name of the list
     */
    public abstract String getListName(final String command);

    /**
     * Check an item.
     * This should return a ListOption for the given input.
     *
     * @param command The command passed as param[0]
     * @param input The input to validate
     * @param user UserSocket that wants to make the change.
     * @return ListOption for this parameter.
     */
    public ListOption checkItem(final String command, final String input, final UserSocket user) {
        return new ListOption(true, input, null);
    }

    /**
     * Get the output to give for an "add", "edit" or "ins" request without sufficient parameters
     *
     * @param command Command to get usage info for (add, edit, ins)
     * @return The output to give
     */
    public abstract String[] getUsageOutput(final String command);

    /**
     * Get the output to give for /dfbnc <command> on its own.
     * Returning null gives default output.
     *
     * @param command Command to get output for
     * @return The output to give
     */
    public String[] getHelpOutput(final String command) {
        return null;
    }

    /**
     * Get the output to give for the syntax to add/edit commands to show valid input
     *
     * @return The output to give for the syntax to add/edit commands to show valid input
     */
    public abstract String getAddUsageSyntax();

    /**
     * Can this list be added to?
     * (This also disables edit and insert)
     *
     * @param command Command to get output for
     * @return If this list can be added to.
     */
    public boolean canAdd(final String command) {
        return true;
    }

    /**
     * Does this command handle multiple sub-lists?
     * If so, the usage changes from "command <add|ins..>" to
     * "command sublist <add|ins..>"
     * This also changes which parameter is passed to canAdd, getListName etc.
     * (They will get the sublist name, not the command name!)
     *
     * @return If this list can be added to.
     */
    public boolean hasSubList() {
        return false;
    }

    /**
     * Does this command allow "--global" as a sub-list rather than a real
     * sub list.
     *
     * This does nothing if hasSubList is false.
     *
     * @return True if --global should be considered as a global list rather
     *         than a sub-list called "--global"
     */
    public boolean allowGlobalList() {
        return false;
    }

    /**
     * Does this command create lists on-the-fly?
     *
     * @return If this list can be added to.
     */
    public boolean isDynamicList() {
        return false;
    }

    /**
     * Is the given parameter a valid sublist? If so, return the name of the
     * sublist, else return null.
     *
     * @param command Sublist name to check.
     * @return Null if this is not a valid sublist, else the name of the list.
     */
    public String validSubList(final String command) {
        return command;
    }

    /**
     * Handle this command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {
        String[] actualParams = params;

        final boolean hasSubList = hasSubList();
        final boolean allowGlobalList = allowGlobalList();
        final int listParam = hasSubList ? 1 : 0;
        final int commandParam = listParam + 1;
        final int positionParam = commandParam + 1;

        if (actualParams.length > commandParam) {
            final String listParamName = hasSubList && (!allowGlobalList || !actualParams[listParam].equalsIgnoreCase("--global")) ? validSubList(actualParams[listParam]) : actualParams[listParam];

            if (listParamName == null) {
                output.addBotMessage("There is no list to modify using '%s'", listParamName);
                output.addBotMessage("Please try /dfbnc '%s' for more information", actualParams[0]);
                return;
            }

            actualParams[commandParam] = getFullParam(output, actualParams, commandParam, Arrays.asList("list", "add", "set", "edit", "delete", "clear", "insert"));
            if (actualParams[commandParam] == null) { return; }

            if (getPropertyName(listParamName).equals("")) {
                output.addBotMessage("There is no list to modify using '%s'", listParamName);
                output.addBotMessage("Please try /dfbnc '%s' for more information", actualParams[0]);
                return;
            }
            final String subListName = hasSubList && (!allowGlobalList || !actualParams[listParam].equalsIgnoreCase("--global")) ? validSubList(actualParams[listParam]) : null;
            List<String> myList = new ArrayList<>();
            if (getConfig(user, subListName).hasOption(getDomainName(listParamName), getPropertyName(listParamName))) {
                myList = getConfig(user, subListName).getOptionList(getDomainName(listParamName), getPropertyName(listParamName));
            } else if (!isDynamicList()) {
                output.addBotMessage("There is no list to modify using '%s'", listParamName);
                output.addBotMessage("Please try /dfbnc '%s' for more information", actualParams[0]);
                return;
            }
            if (actualParams[commandParam].equalsIgnoreCase("list")) {
                if (myList.size() > 0) {
                    output.addBotMessage("You currently have the following items in your %s:", getListName(listParamName));
                    for (int i = 0; i < myList.size(); ++i) {
                        output.addBotMessage("    %2d: %s", i, myList.get(i));
                    }
                } else {
                    output.addBotMessage("Your %s is currently empty.", getListName(listParamName));
                }
            } else if (canAdd(listParamName) && (actualParams[commandParam].equalsIgnoreCase("add") || actualParams[commandParam].equalsIgnoreCase("edit") || actualParams[commandParam].equalsIgnoreCase("ins"))) {
                if (user.isReadOnly()) {
                    output.addBotMessage("Sorry, read-only sub-clients are unable to make changes to lists.");
                    return;
                }

                int numParams = 3;
                if (actualParams[commandParam].equalsIgnoreCase("add")) {
                    numParams = 2;
                }
                if (hasSubList) { numParams += 1; }

                if (actualParams.length > numParams) {
                    int position = -1;
                    if (!actualParams[commandParam].equalsIgnoreCase("add")) {
                        try {
                            position = Integer.parseInt(actualParams[positionParam]);
                            if (position >= myList.size()) {
                                output.addBotMessage("'%s' is not a valid position in the %s", actualParams[positionParam], getListName(listParamName));
                            }
                        } catch (NumberFormatException nfe) {
                            output.addBotMessage("'%s' is not a valid position in the %s", actualParams[positionParam], getListName(listParamName));
                        }
                    }
                    StringBuilder allInput = new StringBuilder("");
                    for (int i = numParams; i < actualParams.length; ++i) {
                        allInput.append(actualParams[i]).append(" ");
                    }
                    ListOption listOption = checkItem(listParamName, allInput.toString().trim(), user);
                    if (listOption.isValid()) {
                        if (actualParams[commandParam].equalsIgnoreCase("add")) {
                            myList.add(listOption.getParam());
                            output.addBotMessage("'%s' has been added to your %s", listOption.getParam(), getListName(listParamName));
                        } else if (actualParams[commandParam].equalsIgnoreCase("edit")) {
                            myList.remove(position);
                            myList.add(position, listOption.getParam());
                            output.addBotMessage("'%s' has been edited to '%s'.", position, listOption.getParam());
                        } else {
                            myList.add(position, listOption.getParam());
                            output.addBotMessage("'%s' has been inserted in position '%s'.", listOption.getParam(), position);
                        }
                    } else {
                        for (String out : listOption.getOutput()) {
                            output.addBotMessage("%s", out);
                        }
                    }
                } else {
                    for (String out : getUsageOutput(actualParams[commandParam])) {
                        output.addBotMessage("%s", out);
                    }
                }
            } else if (actualParams[commandParam].equalsIgnoreCase("del") || actualParams[commandParam].equalsIgnoreCase("delete")) {
                if (user.isReadOnly()) {
                    output.addBotMessage("Sorry, read-only sub-clients are unable to make changes to lists.");
                    return;
                }

                if (actualParams.length > positionParam) {
                    try {
                        final int position = Integer.parseInt(actualParams[positionParam]);
                        if (position < myList.size()) {
                            final String itemName = myList.get(position);
                            myList.remove(position);
                            output.addBotMessage("Item number %s (%s) has been removed from your %s.", position, itemName, getListName(listParamName));
                        } else {
                            output.addBotMessage("There is no item with the number %s in your %s", position, getListName(listParamName));
                            output.addBotMessage("Use /dfbnc %s list to view your %s", actualParams[0], getListName(listParamName));
                        }
                    } catch (NumberFormatException nfe) {
                        output.addBotMessage("You must specify an item number to delete");
                    }
                } else {
                    output.addBotMessage("You must specify an item number to delete");
                }
            } else if (actualParams[commandParam].equalsIgnoreCase("clear")) {
                if (user.isReadOnly()) {
                    output.addBotMessage("Sorry, read-only sub-clients are unable to make changes to lists.");
                    return;
                }
                myList.clear();
                output.addBotMessage("Your %s has been cleared.", getListName(listParamName));
            } else {
                output.addBotMessage("Invalid subcommand: %s", actualParams[commandParam]);
                output.addBotMessage("For assistance, please try: /dfbnc %s", actualParams[0]);
            }
            if (isDynamicList()) {
                if (!myList.isEmpty() || getConfig(user, subListName).hasOption(getDomainName(listParamName), getPropertyName(listParamName))) {
                    // Only save lists that actually have something, or existed before.
                    getConfig(user, subListName).setOption(getDomainName(listParamName), getPropertyName(listParamName), myList);
                } else if (myList.isEmpty()) {
                    getConfig(user, subListName).unsetOption(getDomainName(listParamName), getPropertyName(listParamName));
                }
            } else {
                getConfig(user, subListName).setOption(getDomainName(listParamName), getPropertyName(listParamName), myList);
            }
        } else if (hasSubList && actualParams.length == 1) {
            output.addBotMessage("You must specify a sublist to edit eg:");
            if (allowGlobalList()) {
                output.addBotMessage("  /dfbnc %s --global <command>", actualParams[0]);
            }
            output.addBotMessage("  /dfbnc %s <sublist> <command>", actualParams[0]);
        } else {
            final String listParamName = hasSubList && (!allowGlobalList || !actualParams[listParam].equalsIgnoreCase("--global")) ? validSubList(actualParams[listParam]) : actualParams[listParam];
            String[] helpOutput = getHelpOutput(listParamName);
            if (helpOutput != null) {
                for (final String out : helpOutput) {
                    output.addBotMessage("%s", out);
                }
            } else {
                output.addBotMessage("This command can be used to modify your %s using the following params:", getListName(listParamName));
                final String extra = hasSubList ? " "+listParamName : "";
                output.addBotMessage("  /dfbnc %s%s list", actualParams[0], extra);
                if (canAdd(actualParams[0])) {
                    output.addBotMessage("  /dfbnc %s%s add %s", actualParams[0], extra, getAddUsageSyntax());
                    output.addBotMessage("  /dfbnc %s%s edit <number> %s", actualParams[0], extra, getAddUsageSyntax());
                    output.addBotMessage("  /dfbnc %s%s ins <number> %s", actualParams[0], extra, getAddUsageSyntax());
                }
                output.addBotMessage("  /dfbnc %s%s del <number>", actualParams[0], extra);
                output.addBotMessage("  /dfbnc %s%s clear", actualParams[0], extra);
            }
        }
    }

    /**
     * Get the config object to use for this AbstractSetCommand.
     *
     * The default implementation of this uses the global account config, but
     * sub-implementations may wish to use the subclient config based on the
     * list or sub-list.
     *
     * @param user Usersocket that initiated this list edit
     * @param sublist List we are editing (null if not a sub-list)
     * @return Config
     */
    public Config getConfig(final UserSocket user, final String sublist) {
        return user.getAccountConfig();
    }

    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public abstract String[] handles();

    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public AbstractListEditCommand(final CommandManager manager) {
        super(manager);
    }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public abstract String getDescription(final String command);
}

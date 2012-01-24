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

import uk.org.dataforce.dfbnc.sockets.UserSocket;

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
     * @return ListOption for this parameter.
     */
    public ListOption checkItem(final String command, final String input) {
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
     * @param command Command to get output for
     * @return If this list can be added to.
     */
    public boolean hasSubList() {
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
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        String[] actualParams = params;

        final boolean hasSubList = hasSubList();
        final int listParam = hasSubList ? 1 : 0;
        final int commandParam = listParam + 1;
        final int positionParam = commandParam + 1;

        user.sendBotMessage("----------------");
        if (actualParams.length > commandParam) {

            final String listParamName = hasSubList ? validSubList(actualParams[listParam]) : actualParams[listParam];
            if (listParamName == null) {
                user.sendBotMessage("There is no list to modify using '" + listParamName + "'");
                user.sendBotMessage("Please try /dfbnc '" + actualParams[0] + "' for more information");
                return;
            }

            actualParams[commandParam] = getFullParam(user, actualParams, commandParam, Arrays.asList("list", "add", "set", "edit", "delete", "clear", "insert"));
            if (actualParams[commandParam] == null) { return; }

            if (getPropertyName(listParamName).equals("")) {
                user.sendBotMessage("There is no list to modify using '" + listParamName + "'");
                user.sendBotMessage("Please try /dfbnc '" + actualParams[0] + "' for more information");
                return;
            }
            List<String> myList = new ArrayList<String>();
            myList = user.getAccount().getConfig().getListOption(getDomainName(listParamName), getPropertyName(listParamName), myList);
            if (actualParams[commandParam].equalsIgnoreCase("list")) {
                if (myList.size() > 0) {
                    user.sendBotMessage("You currently have the following items in your " + getListName(listParamName) + ":");
                    for (int i = 0; i < myList.size(); ++i) {
                        user.sendBotMessage(String.format("    %2d: %s", i, myList.get(i)));
                    }
                } else {
                    user.sendBotMessage("Your " + getListName(listParamName) + " is currently empty.");
                }
            } else if (canAdd(listParamName) && (actualParams[commandParam].equalsIgnoreCase("add") || actualParams[commandParam].equalsIgnoreCase("edit") || actualParams[commandParam].equalsIgnoreCase("ins"))) {
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
                                user.sendBotMessage("'" + actualParams[positionParam] + "' is not a valid position in the " + getListName(listParamName));
                            }
                        } catch (NumberFormatException nfe) {
                            user.sendBotMessage("'" + actualParams[positionParam] + "' is not a valid position in the " + getListName(listParamName));
                        }
                    }
                    StringBuilder allInput = new StringBuilder("");
                    for (int i = numParams; i < actualParams.length; ++i) {
                        allInput.append(actualParams[i]).append(" ");
                    }
                    ListOption listOption = checkItem(listParamName, allInput.toString().trim());
                    if (listOption.isValid()) {
                        if (actualParams[commandParam].equalsIgnoreCase("add")) {
                            myList.add(listOption.getParam());
                            user.sendBotMessage("'" + listOption.getParam() + "' has been added to your " + getListName(listParamName));
                        } else if (actualParams[commandParam].equalsIgnoreCase("edit")) {
                            myList.remove(position);
                            myList.add(position, listOption.getParam());
                            user.sendBotMessage("'" + position + "' has been edited to '" + listOption.getParam() + "'.");
                        } else {
                            myList.add(position, listOption.getParam());
                            user.sendBotMessage("'" + listOption.getParam() + "' has been inserted in position '" + position + "'.");
                        }
                    } else {
                        for (String out : listOption.getOutput()) {
                            user.sendBotMessage(out);
                        }
                    }
                } else {
                    for (String out : getUsageOutput(actualParams[commandParam])) {
                        user.sendBotMessage(out);
                    }
                }
            } else if (actualParams[commandParam].equalsIgnoreCase("del") || actualParams[commandParam].equalsIgnoreCase("delete")) {
                if (actualParams.length > positionParam) {
                    try {
                        final int position = Integer.parseInt(actualParams[positionParam]);
                        if (position < myList.size()) {
                            final String itemName = myList.get(position);
                            myList.remove(position);
                            user.sendBotMessage("Item number " + position + " (" + itemName + ") has been removed from your " + getListName(listParamName) + ".");
                        } else {
                            user.sendBotMessage("There is no item with the number " + position + " in your " + getListName(listParamName));
                            user.sendBotMessage("Use /dfbnc " + actualParams[0] + " list to view your " + getListName(listParamName));
                        }
                    } catch (NumberFormatException nfe) {
                        user.sendBotMessage("You must specify an item number to delete");
                    }
                } else {
                    user.sendBotMessage("You must specify an item number to delete");
                }
            } else if (actualParams[commandParam].equalsIgnoreCase("clear")) {
                myList.clear();
                user.sendBotMessage("Your " + getListName(listParamName) + " has been cleared.");
            } else {
                user.sendBotMessage("Invalid subcommand: " + actualParams[commandParam]);
                user.sendBotMessage("For assistance, please try: /dfbnc " + actualParams[0]);
            }
            user.getAccount().getConfig().setListOption(getDomainName(listParamName), getPropertyName(listParamName), myList);
        } else if (hasSubList && actualParams.length == 1) {
            user.sendBotMessage("You must specify a sublist to edit eg:");
            user.sendBotMessage("  /dfbnc " + actualParams[0] + " <sublist> <command>");
        } else {
            final String listParamName = hasSubList ? validSubList(actualParams[listParam]) : actualParams[listParam];
            String[] output = getHelpOutput(listParamName);
            if (output != null) {
                for (String out : output) {
                    user.sendBotMessage(out);
                }
            } else {
                user.sendBotMessage("This command can be used to modify your " + getListName(listParamName) + " using the following params:");
                final String extra = hasSubList ? " "+listParamName : "";
                user.sendBotMessage("  /dfbnc " + actualParams[0] + extra +" list");
                if (canAdd(actualParams[0])) {
                    user.sendBotMessage("  /dfbnc " + actualParams[0] + extra + " add " + getAddUsageSyntax());
                    user.sendBotMessage("  /dfbnc " + actualParams[0] + extra + " edit <number> " + getAddUsageSyntax());
                    user.sendBotMessage("  /dfbnc " + actualParams[0] + extra + " ins <number> " + getAddUsageSyntax());
                }
                user.sendBotMessage("  /dfbnc " + actualParams[0] + extra + " del <number>");
                user.sendBotMessage("  /dfbnc " + actualParams[0] + extra + " clear");
            }
        }
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

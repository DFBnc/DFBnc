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
     * Handle this command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public final void handle(final UserSocket user, final String[] params) {
        String[] actualParams = params;

        user.sendBotMessage("----------------");
        if (actualParams.length > 1) {
            
            actualParams[1] = getFullParam(user, actualParams, 1, Arrays.asList("list", "add", "set", "edit", "delete", "clear", "insert"));
            if (actualParams[1] == null) { return; }
            
            if (getPropertyName(actualParams[0]).equals("")) {
                user.sendBotMessage("There is no list to modify using '" + actualParams[0] + "'");
                user.sendBotMessage("Please try /dfbnc '" + actualParams[0] + "' for more information");
                return;
            }
            List<String> myList = new ArrayList<String>();
            myList = user.getAccount().getConfig().getListOption(getDomainName(actualParams[0]), getPropertyName(actualParams[0]), myList);
            if (actualParams[1].equalsIgnoreCase("list")) {
                if (myList.size() > 0) {
                    user.sendBotMessage("You currently have the following items in your " + getListName(actualParams[0]) + ":");
                    for (int i = 0; i < myList.size(); ++i) {
                        user.sendBotMessage(String.format("    %2d: %s", i, myList.get(i)));
                    }
                } else {
                    user.sendBotMessage("Your " + getListName(actualParams[0]) + " is currently empty.");
                }
            } else if (canAdd(actualParams[0]) && (actualParams[1].equalsIgnoreCase("add") || actualParams[1].equalsIgnoreCase("edit") || actualParams[1].equalsIgnoreCase("ins"))) {
                int numParams = 3;
                if (actualParams[1].equalsIgnoreCase("add")) {
                    numParams = 2;
                }
                if (actualParams.length > numParams) {
                    int position = -1;
                    if (!actualParams[1].equalsIgnoreCase("add")) {
                        try {
                            position = Integer.parseInt(actualParams[2]);
                            if (position >= myList.size()) {
                                user.sendBotMessage("'" + actualParams[2] + "' is not a valid position in the " + getListName(actualParams[0]));
                            }
                        } catch (NumberFormatException nfe) {
                            user.sendBotMessage("'" + actualParams[2] + "' is not a valid position in the " + getListName(actualParams[0]));
                        }
                    }
                    StringBuilder allInput = new StringBuilder("");
                    for (int i = numParams; i < actualParams.length; ++i) {
                        allInput.append(actualParams[i]).append(" ");
                    }
                    ListOption listOption = checkItem(actualParams[0], allInput.toString().trim());
                    if (listOption.isValid()) {
                        if (actualParams[1].equalsIgnoreCase("add")) {
                            myList.add(listOption.getParam());
                            user.sendBotMessage("'" + listOption.getParam() + "' has been added to your " + getListName(actualParams[0]));
                        } else if (actualParams[1].equalsIgnoreCase("edit")) {
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
                    for (String out : getUsageOutput(actualParams[1])) {
                        user.sendBotMessage(out);
                    }
                }
            } else if (actualParams[1].equalsIgnoreCase("del") || actualParams[1].equalsIgnoreCase("delete")) {
                if (actualParams.length > 2) {
                    try {
                        final int position = Integer.parseInt(actualParams[2]);
                        if (position < myList.size()) {
                            final String itemName = myList.get(position);
                            myList.remove(position);
                            user.sendBotMessage("Item number " + position + " (" + itemName + ") has been removed from your " + getListName(actualParams[0]) + ".");
                        } else {
                            user.sendBotMessage("There is no item with the number " + position + " in your " + getListName(actualParams[0]));
                            user.sendBotMessage("Use /dfbnc " + actualParams[0] + " list to view your " + getListName(actualParams[0]));
                        }
                    } catch (NumberFormatException nfe) {
                        user.sendBotMessage("You must specify an item number to delete");
                    }
                } else {
                    user.sendBotMessage("You must specify an item number to delete");
                }
            } else if (actualParams[1].equalsIgnoreCase("clear")) {
                myList.clear();
                user.sendBotMessage("Your " + getListName(actualParams[0]) + " has been cleared.");
            } else {
                user.sendBotMessage("Invalid subcommand: " + actualParams[1]);
                user.sendBotMessage("For assistance, please try: /dfbnc " + actualParams[0]);
            }
            user.getAccount().getConfig().setListOption(getDomainName(actualParams[0]), getPropertyName(actualParams[0]), myList);
        } else {
            String[] output = getHelpOutput(actualParams[0]);
            if (output != null) {
                for (String out : output) {
                    user.sendBotMessage(out);
                }
            } else {
                user.sendBotMessage("This command can be used to modify your " + getListName(actualParams[0]) + " using the following params:");
                user.sendBotMessage("  /dfbnc " + actualParams[0] + " list");
                if (canAdd(actualParams[0])) {
                    user.sendBotMessage("  /dfbnc " + actualParams[0] + " add " + getAddUsageSyntax());
                    user.sendBotMessage("  /dfbnc " + actualParams[0] + " edit <number> " + getAddUsageSyntax());
                    user.sendBotMessage("  /dfbnc " + actualParams[0] + " ins <number> " + getAddUsageSyntax());
                }
                user.sendBotMessage("  /dfbnc " + actualParams[0] + " del <number>");
                user.sendBotMessage("  /dfbnc " + actualParams[0] + " clear");
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

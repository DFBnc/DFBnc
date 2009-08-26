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

import uk.org.dataforce.dfbnc.sockets.UserSocket;

import java.util.ArrayList;
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
        user.sendBotMessage("----------------");
        if (params.length > 1) {
            if (getPropertyName(params[0]).equals("")) {
                user.sendBotMessage("There is no list to modify using '" + params[0] + "'");
                user.sendBotMessage("Please try /dfbnc '" + params[0] + "' for more information");
                return;
            }
            List<String> myList = new ArrayList<String>();
            myList = user.getAccount().getConfig().getListOption(getDomainName(params[0]), getPropertyName(params[0]), myList);
            if (params[1].equalsIgnoreCase("list")) {
                if (myList.size() > 0) {
                    user.sendBotMessage("You currently have the following items in your " + getListName(params[0]) + ":");
                    for (int i = 0; i < myList.size(); ++i) {
                        user.sendBotMessage(String.format("    %2d: %s", i, myList.get(i)));
                    }
                } else {
                    user.sendBotMessage("Your " + getListName(params[0]) + " is currently empty.");
                }
            } else if (canAdd(params[0]) && (params[1].equalsIgnoreCase("add") || params[1].equalsIgnoreCase("edit") || params[1].equalsIgnoreCase("ins"))) {
                int numParams = 3;
                if (params[1].equalsIgnoreCase("add")) {
                    numParams = 2;
                }
                if (params.length > numParams) {
                    int position = -1;
                    if (!params[1].equalsIgnoreCase("add")) {
                        try {
                            position = Integer.parseInt(params[2]);
                            if (position >= myList.size()) {
                                user.sendBotMessage("'" + params[2] + "' is not a valid position in the " + getListName(params[0]));
                            }
                        } catch (NumberFormatException nfe) {
                            user.sendBotMessage("'" + params[2] + "' is not a valid position in the " + getListName(params[0]));
                        }
                    }
                    StringBuilder allInput = new StringBuilder("");
                    for (int i = numParams; i < params.length; ++i) {
                        allInput.append(params[i] + " ");
                    }
                    ListOption listOption = checkItem(params[0], allInput.toString().trim());
                    if (listOption.isValid()) {
                        if (params[1].equalsIgnoreCase("add")) {
                            myList.add(listOption.getParam());
                            user.sendBotMessage("'" + listOption.getParam() + "' has been added to your " + getListName(params[0]));
                        } else if (params[1].equalsIgnoreCase("edit")) {
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
                    for (String out : getUsageOutput(params[1])) {
                        user.sendBotMessage(out);
                    }
                }
            } else if (params[1].equalsIgnoreCase("del") || params[1].equalsIgnoreCase("delete")) {
                if (params.length > 2) {
                    try {
                        final int position = Integer.parseInt(params[2]);
                        if (position < myList.size()) {
                            final String itemName = myList.get(position);
                            myList.remove(position);
                            user.sendBotMessage("Item number " + position + " (" + itemName + ") has been removed from your " + getListName(params[0]) + ".");
                        } else {
                            user.sendBotMessage("There is no item with the number " + position + " in your " + getListName(params[0]));
                            user.sendBotMessage("Use /dfbnc " + params[0] + " list to view your " + getListName(params[0]));
                        }
                    } catch (NumberFormatException nfe) {
                        user.sendBotMessage("You must specify an item number to delete");
                    }
                } else {
                    user.sendBotMessage("You must specify an item number to delete");
                }
            } else if (params[1].equalsIgnoreCase("clear")) {
                myList.clear();
                user.sendBotMessage("Your " + getListName(params[0]) + " has been cleared.");
            }
            user.getAccount().getConfig().setListOption(getDomainName(params[0]), getPropertyName(params[0]), myList);
        } else {
            String[] output = getHelpOutput(params[0]);
            if (output != null) {
                for (String out : output) {
                    user.sendBotMessage(out);
                }
            } else {
                user.sendBotMessage("This command can be used to modify your " + getListName(params[0]) + " using the following params:");
                user.sendBotMessage("  /dfbnc " + params[0] + " list");
                if (canAdd(params[0])) {
                    user.sendBotMessage("  /dfbnc " + params[0] + " add " + getAddUsageSyntax());
                    user.sendBotMessage("  /dfbnc " + params[0] + " edit <number> " + getAddUsageSyntax());
                    user.sendBotMessage("  /dfbnc " + params[0] + " ins <number> " + getAddUsageSyntax());
                }
                user.sendBotMessage("  /dfbnc " + params[0] + " del <number>");
                user.sendBotMessage("  /dfbnc " + params[0] + " clear");
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

    /**
     * Get SVN information.
     *
     * @return SVN String
     */
    public static String getSvnInfo() {
        return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $";
    }
}

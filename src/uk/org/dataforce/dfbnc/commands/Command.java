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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import uk.org.dataforce.dfbnc.DFBnc;
import uk.org.dataforce.dfbnc.sockets.UserSocket;

/**
 * This file represents a command
 */
public abstract class Command {
    /** Reference to the CommandManager in charge of this Command. */
    protected CommandManager myManager;

    /**
     * Create a new instance of the Command Object.
     *
     * @param manager CommandManager that is in charge of this Command
     */
    protected Command(final CommandManager manager) {
        this.myManager = manager;
    }

    /**
     * Handle a Line.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    public abstract void handle(final UserSocket user, final String[] params);

    /**
     * What does this Command handle.
         * Aliases should be prefixed with * to hide from showcommands
     *
     * @return String[] with the names of the tokens we handle.
     */
    public abstract String[] handles();

    /**
     * Get detailed help for this command.
     *
     * @param params Parameters the user wants help with.
     *               params[0] will be the command name.
     * @return String[] with the lines to send to the user as the help, or null
     *         if no detailed help is available.
     */
    public String[] getHelp(final String[] params) {
        return null;
    }

    /**
     * Get the name for this Command.
     * @return the name of this Command
     */
    public final String getName() {
        final Package thisPackage = this.getClass().getPackage();
        int packageLength = 0;
        if (thisPackage != null) {
            packageLength = thisPackage.getName().length() + 1;
        }
        return this.getClass().getName().substring(packageLength);
    }

    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    public abstract String getDescription(final String command);

    /**
     * Is this command Admin only?
     *
     * @return true if admin-only command
     */
    public boolean isAdminOnly() {
        return false;
    }

    /**
     * Can this command be run when called in short form when shortcommands are
     * enabled?
     *
     * @param handle Handle that is being asked for
     * @return true if short-form is allowed for this command.
     */
    public boolean allowShort(final String handle) {
        return true;
    }

    /**
     * Get the name for this Command in lowercase.
     *
     * @return lower case name of this Command
     */
    public final String getLowerName() {
        return this.getName().toLowerCase();
    }

    /**
     * From a given list of possible params, and a given param, find any
     * possible matching params.
     *
     * The returned list will ALWAYS contain at least 1 item (which may or
     * may not be empty)
     *
     * - If allowshortcommands is off, then the given param is returned
     *   (regardless of whether it is null or empty.)
     * - If an exact match is found, it is returned.
     * - If param is null, empty or "?" then the given list of params is
     *   returned.
     * - If param is not null or empty then any params from the list that start
     *   case-insentiviely with param are returned (although the actual case of
     *   the param in the list is used in the result);
     * - If there are no matches, the given param is returned
     *
     * @param param Param given
     * @param params Collection of params to check against
     * @return List of matching params.
     */
    public List<String> getParamMatch(final String param, final Collection<String> params) {
        if (!DFBnc.getBNC().getConfig().getBoolOption("general", "allowshortcommands", false)) {
            return Arrays.asList(param);
        }
        final String sw = param.toLowerCase();

        if (params.contains(param)) {
            return Arrays.asList(param);
        } else if(param == null || param.isEmpty() || param.equals("?")) {
            return new ArrayList<String>(params);
        } else {
            final List<String> result = new ArrayList<String>();
            for (String p : params) {
                p = p.toLowerCase();
                if (!result.contains(p) && p.startsWith(sw)) {
                    result.add(p);
                }
            }

            if (result.isEmpty()) {
                result.add(param);
            }

            return result;
        }
    }

    /**
     * From the given params check the parameter in the given position to see
     * if it is one of the given options.
     *
     * If there are multiple possibilities, let the user know and return null,
     * otherwise return the full version of the parameter if it matches, or the
     * input the user gave.
     *
     * @param user User to send possible matches to
     * @param params Parameters for the command
     * @param position Position to check
     * @param options Options to look for
     * @return null or a string containing the full param or the input param.
     */
    public final String getFullParam(final UserSocket user, final String[] params, final int position, final Collection<String> options) {
        String result = (params.length > position) ? params[position] : "";
        final List<String> paramMatch = getParamMatch(result, options);
        boolean hasEmpty = false;
        if (paramMatch.size() > 1) {
            user.sendBotMessage("Multiple possible matches were found for '"+result+"': ");
            for (String p : paramMatch) {
                if (p.isEmpty()) {
                    hasEmpty = true;
                } else {
                    user.sendBotMessage("    " + p);
                }
            }
            if (hasEmpty) {
                user.sendBotMessage("    <cr>");
            }
            return null;
        } else { result = paramMatch.get(0); }

        return result;
    }

    /**
     * Get the name for this Command.
     * @return the name of this Command
     */
    @Override
    public final String toString() { return this.getName(); }
}
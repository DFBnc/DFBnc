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
    public final boolean isAdminOnly() {
        return false;
    }
    
    /**
     * Get the name for this Command in lowercase.
     * @return lower case name of this Command
     */
    public final String getLowerName() {
        return this.getName().toLowerCase();
    }
    
    /**
     * Get the name for this Command.
     * @return the name of this Command
     */
    @Override
    public final String toString() { return this.getName(); }
    
    /**
     * Get SVN Version information.
     *
     * @return SVN Version String
     */
    public static String getSvnInfo() { return "$Id: Command.java 1320 2007-05-21 09:53:01Z ShaneMcC $"; }    
}
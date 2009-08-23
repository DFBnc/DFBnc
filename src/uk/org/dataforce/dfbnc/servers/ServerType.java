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
package uk.org.dataforce.dfbnc.servers;

import uk.org.dataforce.dfbnc.Account;
import uk.org.dataforce.dfbnc.UserSocket;
import uk.org.dataforce.dfbnc.UserSocketWatcher;

/**
 * This file represents a ServerType
 */
public abstract class ServerType implements UserSocketWatcher {
    /** Reference to the ServerTypeManager in charge of this ServerType. */
    protected ServerTypeManager myManager;

    /**
     * Create a new instance of the ServerType Object.
     *
     * @param manager ServerTypeManager that is in charge of this ServerType
     */
    protected ServerType(final ServerTypeManager manager) {
        this.myManager = manager;
    }
    
    /**
     * Get the name for this ServerType.
     *
     * @return the name of this ServerType
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
     * Get the name for this ServerType in lowercase.
     *
     * @return lower case name of this ServerType
     */
    public final String getLowerName() {
        return this.getName().toLowerCase();
    }
    
    /**
     * Get the Description for this ServerType
     *
     * @return lower case name of this ServerType
     */
    public abstract String getDescription();
    
    /**
     * Called when this ServerType is activated
     *
     * @param account Account that activated the servertype
     */
    public abstract void activate(final Account account);
    
    /**
     * Called when this ServerType is deactivated
     *
     * @param account Account that deactivated the servertype
     */
    public abstract void deactivate(final Account account);
    
    /**
     * Called when a new UserSocket is opened on an account that this class is
     * linked to.
     *
     * @param user UserSocket for user
     */
    @Override
    public void userConnected(final UserSocket user) { }
    
    /**
     * Called when a UserSocket is closed on an account that this class is
     * linked to.
     *
     * @param user UserSocket for user
     */
    @Override
    public void userDisconnected(final UserSocket user) { }
    
    /**
     * Called to close any Active connections.
     * This is called when an account is being disabled/removed or the BNC
     * is shutting down.
     *
     * @param account Account to handle close for.
     * @param reason Reason for closing.
     */
    public abstract void close(final Account account, final String reason);
    
    /**
     * Get the name for this ServerType.
     *
     * @return the name of this ServerType
     */
    @Override
    public final String toString() { return this.getName(); }
    
    /**
     * Get SVN Version information.
     *
     * @return SVN Version String
     */
    public static String getSvnInfo() { return "$Id: ServerType.java 1320 2007-05-21 09:53:01Z ShaneMcC $"; }    
}
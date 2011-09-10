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
package uk.org.dataforce.dfbnc;

import uk.org.dataforce.dfbnc.sockets.UnableToConnectException;
import uk.org.dataforce.dfbnc.sockets.UserSocket;

/**
 * This file represents a ConnectionHandler
 */
public interface ConnectionHandler {
    /**
     * Shutdown this ConnectionHandler
     *
     * @param reason Reason for the Shutdown
     */
    public void shutdown(final String reason);
    
    /**
     * Get the users host on this connection
     *
     * @return The users host on this connect
     */
    public String getMyHost();
    
    /**
     * Called when data is recieved on the user socket.
     *
     * @param user The socket that the data arrived on
     * @param data Un-tokenised version of the Data that was recieved
     * @param line IRC-Tokenised version of the Data that was recieved
     */
    public void dataRecieved(final UserSocket user, final String data, final String[] line);
    
    /**
     * Servername to use in sendIRCLine and Bot SNOTICEs as an alternative to
     * Functions.getServerName() or null if no change.
     *
     * @return servername to use.
     */
    public String getServerName();

    /**
     * Generate a new instance of this ConnectionHandler with the same
     * parameters used for construction previously.
     * This is not a clone of the ConnectionHandler, but is suitable for
     * reconnecting the connection.
     *
     * @return New instance of tihs ConnectionHandler.
     */
    public ConnectionHandler newInstance() throws UnableToConnectException;
}
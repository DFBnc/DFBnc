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
package com.dfbnc;

import com.dfbnc.servers.logging.ServerLogger;
import com.dfbnc.sockets.DebugFlag;
import com.dfbnc.util.BackbufferMessage;
import com.dfbnc.util.RollingList;
import com.dfbnc.sockets.UnableToConnectException;

import com.dmdirc.parser.interfaces.Parser;

import com.dfbnc.sockets.UserSocket;

/**
 * This file represents a ConnectionHandler
 */
public interface ConnectionHandler {

    /**
     * Initialises the connection handler, begins connecting to servers,
     * and starts relevant timers.
     *
     * @throws UnableToConnectException If there is a problem connecting to
     * the server
     */
    void init() throws UnableToConnectException;

    /**
     * Shutdown this ConnectionHandler
     *
     * @param reason Reason for the Shutdown
     */
    void shutdown(final String reason);

    /**
     * Get the users host on this connection
     *
     * @return The users host on this connect
     */
    String getMyHost();

    /**
     * Called when data is received on the user socket.
     *
     * @param user The socket that the data arrived on
     * @param data Un-tokenised version of the Data that was received
     * @param line IRC-Tokenised version of the Data that was received
     */
    void dataReceived(final UserSocket user, final String data, final String[] line);

    /**
     * Servername to use in sendIRCLine and Bot SNOTICEs as an alternative to
     * Functions.getServerName() or null if no change.
     *
     * @return servername to use.
     */
    String getServerName();

    /**
     * Generate a new instance of this ConnectionHandler with the same
     * parameters used for construction previously.
     * This is not a clone of the ConnectionHandler, but is suitable for
     * reconnecting the connection.
     *
     * @return New instance of tihs ConnectionHandler.
     */
    ConnectionHandler newInstance() throws UnableToConnectException;


    /**
     * Get the backbuffer rolling list for the given channel name.
     *
     * @param channel Channel name
     * @return Backbuffer list.
     */
    RollingList<BackbufferMessage> getBackbufferList(final String channel);

    /**
     * Called to make the BNC fake lines to the client on disconnect.
     *
     * This should let the user know that the bnc has been disconnected from
     * the server, and send lines that will make the client take actions to
     * make the disconnect visual to the user. (Eg remove all clients, and/or
     * the user themselves from any channels).
     *
     * This is not the preferred method of doing this, but some people may
     * prefer it over the preferred "disconnect on socket closed" option. This
     * is the behaviour of dfbnc-delphi.
     *
     * The BNC will still send a new 001-005 etc to the client on reconnect
     * which may cause some confusion and state-loss in the client parser,
     * which is why the default is to just  disconnect the user when the socket
     * is closed.
     *
     * @param user User to clean up.
     * @param reason Reason for clean up.
     */
    void cleanupUser(final UserSocket user, final String reason);

    /**
     * Is the given socket allowed to interact with the given channel name on
     * this connection?
     *
     * @param user User Socket to check
     * @param channel Channel Name
     * @return True if this socket is allowed, else false.
     */
    boolean activeAllowedChannel(final UserSocket user, final String channel);

    /**
     * Subscribe to any ConnectionHandler Event Buses.
     *
     * @param listener Listener to subscribe.
     */
    void subscribe(final Object listener);

    /**
     * Unsubscribe from any ConnectionHandler Event Buses.
     *
     * @param listener Listener to subscribe.
     */
    void unsubscribe(final Object listener);

    /**
     * Create a ServerLogger that will deal with events from this ConnectionHandler.
     *
     * @return a ServerLogger that will deal with events from this ConnectionHandler.
     */
    ServerLogger createServerLogger();

    /**
     * Get the parser used by this ConnectionHandler.
     *
     * @return The parser used by this ConnectionHandler.
     */
    Parser getParser();

    /**
     * Enable a DebugFlag on this connection handler.
     * This may be called multiple times for the same flag for each UserSocket
     * that enabled it.
     *
     * @param flag Flag to enable
     */
    void enableDebug(final DebugFlag flag);

    /**
     * Disable a DebugFlag on this connection handler.
     * This should only be called once the flag is disabled on all UserSockets
     * for this account.
     *
     * @param flag Flag to disable.
     */
    void disableDebug(final DebugFlag flag);
}

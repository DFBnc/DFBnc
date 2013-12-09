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

package com.dfbnc.sockets;

import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CountDownLatch;

import com.dfbnc.IRCLine;
import uk.org.dataforce.libs.logger.Logger;

/**
 * This is responsible for taking incoming data, and separating it
  * into "\n" separated lines.
 */
public abstract class ConnectedSocket implements SelectedSocketHandler {

    /** SocketWrapper, used to allow for SSL Sockets */
    protected final SocketWrapper mySocketWrapper;
    /** String to identify socket by */
    private String socketID = "ConnectedSocket";
    /** Has this socket been closed? */
    private boolean isClosed = false;
    /** Are we an SSL Socket? */
    protected final boolean isSSL;
    /** Lock for guarding read/writes to socket wrapper. Urgh. */
    private CountDownLatch socketWrapperLock = new CountDownLatch(1);
    /** What was the reason that this socket closed? */
    private String closeReason = "Unknown reason.";

    /**
     * Create a new ConnectedSocket.
     *
     * @param channel Socket to control
     * @param idstring Name to call this socket.
     * @param fromSSL Did this come from an SSL ListenSocket?
     * @throws IOException If there is a problem creating Socket
     */
    protected ConnectedSocket(final SocketChannel channel, final String idstring, final boolean fromSSL) throws IOException {
        isSSL = fromSSL;

        channel.configureBlocking(false);

        final SelectionKey key = SocketSelector.getConnectedSocketSelector().registerSocket(channel, this);

        if (isSSL) {
            mySocketWrapper = new SecureSocket(channel, this, key);
        } else {
            mySocketWrapper = new PlainSocket(channel, this, key);
        }

        socketWrapperLock.countDown();
    }

    /**
     * Get the reason that this socket closed.
     *
     * @return The reason that this socket closed.
     */
    public String getCloseReason() {
        return closeReason;
    }

    /**
     * Used to close this socket.
     */
    public final void closeSocket(final String reason) {
        closeReason = reason;
        if (isClosed) { return; }
        Logger.info("Connected Socket closing ("+socketID+")");
        isClosed = true;

        // Close the actual socket
        try {
            mySocketWrapper.close();
        } catch (IOException e) {
        }

        this.socketClosed(false);
    }

    /**
     * Is this socket still open?
     *
     * @return True if this socket has not been closed yet.
     */
    public boolean isOpen() {
        return !isClosed;
    }

    /**
     * Get the SocketWrapper this socket uses
     *
     * @return The SocketWrapper this socket uses
     */
    public SocketWrapper getSocketWrapper() {
        try {
            socketWrapperLock.await();
            return mySocketWrapper;
        } catch (InterruptedException ex) {
            Logger.error("Thread interrupted while waiting to get socket wrapper");
            return null;
        }
    }

    /**
     * Set this Sockets ID
     *
     * @param idstring New ID String for this socket
     */
    public void setSocketID(final String idstring) {
        socketID = idstring;
    }

    /**
     * Gets the ID of this socket.
     *
     * @return This socket's ID
     */
    public String getSocketID() {
        return socketID;
    }

    /**
     * Used to send a line of data to this socket, in printf format.
     *
     * @param data The format string
     * @param args The args for the format string
     */
    public final void sendLine(final String data, final Object... args) {
        sendLine(String.format(data, args));
    }

    /**
     * Used to send a line of data to this socket.
     * This adds to the buffer.
     *
     * @param line Line to send
     */
    public final void sendLine(final String line) {
        mySocketWrapper.sendLine(line);
    }

    /**
     * Used to send line(s) of data to this socket.
     * This adds the lines to the buffer.
     *
     * @param lines Lines to send
     */
    public final void sendLine(final IRCLine... lines) {
        for (final IRCLine line : lines) {
            mySocketWrapper.sendLine(line.toString());
        }
    }

    /**
     * Process a line of data.
     *
     * @param line Line to handle
     */
    abstract void processLine(final String line);

    /**
     * Action to take when socket is opened and ready.
     */
    public void socketOpened() {
    }

    /**
     * Action to take when socket is closed.
     *
     * @param userRequested True if socket was closed by the user, false otherwise
     */
    protected void socketClosed(final boolean userRequested) {
    }

    /** {@inheritDoc} */
    @Override
    public void processSelectionKey(final SelectionKey selKey) throws IOException {
        getSocketWrapper().processSelectionKey(selKey);
    }

}

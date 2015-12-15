/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.dfbnc.sockets.secure.SecureSocket;
import uk.org.dataforce.libs.logger.Logger;

/**
 * The ServerSocket for the application.
 * This handles and redirects all incoming connections to UserSockets
 */
public class ListenSocket implements SelectedSocketHandler {

    /** My ServerSocketChannel. */
    private ServerSocketChannel ssChannel = ServerSocketChannel.open();
    /** Is this an ssl listen socket */
    private boolean isSSL;

    /**
     * Create a new ListenSocket.
     *
     * @param listenhost Host/port to listen on (in format host:port)
     * @throws IOException if there is problems with the sockets.
     */
    public ListenSocket(final String listenhost) throws IOException {
        final String bits[] = listenhost.split(":");
        if (bits.length > 1) {
            try {
                boolean ssl = false;
                final int portNum;
                if (bits[1].charAt(0) == '+') {
                    portNum = Integer.parseInt(bits[1].substring(1));
                    ssl = true;
                } else {
                    portNum = Integer.parseInt(bits[1]);
                }
                setupSocket(bits[0], portNum, ssl);
            } catch (NumberFormatException nfe) {
                throw new IOException(bits[1]+" is not a valid port");
            }
        } else {
            throw new IOException(listenhost+" is not a valid listenhost");
        }
    }

    /**
     * Setup the ListenSocket.
     *
     * @param host Hostname to listen on
     * @param port Hostname to listen on
     * @param ssl True/false for ssl
     * @throws IOException if there is problems with the sockets.
     */
    private void setupSocket(final String host, final int port, final boolean ssl) throws IOException {
        // Check that SSL settings are correct.
        // If this throws any exceptions, we have a problem and shouldn't open
        // the socket.
        isSSL = ssl;
        String portString = Integer.toString(port);

        if (ssl) {
            portString = "+" + portString;
        }

        if (ssl) {
            try {
                SecureSocket.getSSLContext();
            } catch (Exception e) {
                Logger.error("Failed to open SSL Socket '" + host + ":" + portString + "'");
                Logger.error("Reason: " + e.getMessage());
                throw new IOException("Unable to use SSL");
            }
        }

        ssChannel.configureBlocking(false);
        ssChannel.socket().bind(new InetSocketAddress(host, port));

        SocketSelector.getConnectedSocketSelector().registerSocket(ssChannel, this);

        Logger.info("Listen Socket Opened: " + host + ":" + portString);
    }

    /**
     * Close the socket
     */
    public synchronized void close() {
        try {
            ssChannel.socket().close();
        } catch (IOException e) {
            Logger.error("Unable to close socket.: " + e.getMessage());
        }
    }

    @Override
    public void processSelectionKey(final SelectionKey selKey) {
        try {
            if (selKey.isAcceptable()) {
                final ServerSocketChannel selChannel = (ServerSocketChannel) selKey.channel();

                try {
                    final SocketChannel sChannel = selChannel.accept();

                    if (sChannel != null) {
                        Logger.info("Accepting new socket.");
                        UserSocket userSocket = new UserSocket(sChannel, isSSL);
                        userSocket.socketOpened();
                    }
                } catch (IOException e) {
                    Logger.error("Unable to open UserSocket: "+e.getMessage());
                }
            }
        } catch (final CancelledKeyException cke) {
            // Key was cancelled, close the socket.
            // (Chances are, the socket is already being closed...)
            close();
        }
    }

}

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

package uk.org.dataforce.dfbnc.sockets;

import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import uk.org.dataforce.libs.logger.Logger;

/**
 * This is responsible for handling the selector for all Sockets when in
 * single-thread mode.
 * This is not used at all in multi-thread mode.
 */
public final class ConnectedSocketSelector implements Runnable {

    /** Used to monitor the sockets. */
    private Selector selector = null;
    /** Thread to run the selector under */
    private Thread myThread = new Thread(this);
    /** Instance of ConnectedSocketSelector in use */
    private static ConnectedSocketSelector myConnectedSocketSelector = null;
    /** Lock for access to the selector. */
    private final Semaphore selectorLock = new Semaphore(1);

    /**
     * Create a new ConnectedSocketSelector.
     */
    private ConnectedSocketSelector() throws IOException {
        selector = Selector.open();

        myThread.setName("Connected Socket Selector Thread");
        myThread.start();
    }

    /**
     * Get the instance of ConnectedSocketSelector
     *
     * @return The instance of ConnectedSocketSelector
     */
    public static synchronized ConnectedSocketSelector getConnectedSocketSelector() {
        if (myConnectedSocketSelector == null) {
            try {
                myConnectedSocketSelector = new ConnectedSocketSelector();
            } catch (IOException e) {
                Logger.error("Error opening socket selector.");
            }
        }
        return myConnectedSocketSelector;
    }


    /**
     * Get the selector in use by this Socket
     *
     * @return The selector in use by this Socket
     */
    public Selector getSelector() {
        return selector;
    }

    /**
     * Register a ConnectedSocket
     *
     * @param channel SocketChannel to register
     * @param socket ConnectedSocket to call for events
     * @throws ClosedChannelException If the channel is closed when trying to register with the selector
     * @return A key representing the registration of the channel
     */
    public SelectionKey registerSocket(final SocketChannel channel, final ConnectedSocket socket) throws ClosedChannelException {
        try {
            selectorLock.acquireUninterruptibly();
            selector.wakeup();

            return channel.register(selector,
                    SelectionKey.OP_READ | SelectionKey.OP_CONNECT,
                    socket);
        } finally {
            selectorLock.release();
        }
    }

    /**
     * Used to actually do stuff.
     */
    @Override
    public final void run() {
        while (myThread == Thread.currentThread()) {
            try {
                // Ensure that we don't start selecting while registerSocket
                // is in the process of trying to register a new channel
                selectorLock.acquireUninterruptibly();
                selectorLock.release();

                selector.select(0);
            } catch (IOException e) {
                break;
            }

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                final SelectionKey selKey = it.next();
                it.remove();

                try {
                    ((ConnectedSocket) selKey.attachment()).getSocketWrapper().processSelectionKey(selKey);
                } catch (IOException e) {
                    selKey.cancel();
                    break;
                }
            }
        }
    }
}

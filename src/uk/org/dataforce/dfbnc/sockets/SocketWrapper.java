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

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.CancelledKeyException;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.org.dataforce.libs.logger.Logger;

/**
 * This defines a basic SocketWrapper
 */
public abstract class SocketWrapper {
    /** Used to process incoming data. */
    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    /** Used to process outgoing data. */
    private StringBuilder lineBuffer = new StringBuilder();
    /** Lines to be sent to the user go into this buffer. */
    protected final StringBuffer outBuffer = new StringBuffer();

    /** The selection key corresponding to the channel's registration */
    private final SelectionKey key;

    /** The ConnectedSocket that owns this Wrapper */
    protected ConnectedSocket myOwner;

    /** My socket channel. */
    protected final SocketChannel mySocketChannel;
    /** My socket channel socket. */
    protected final Socket mySocket;
    /** My Byte Channel. */
    protected ByteChannel myByteChannel;

    /** Are we currently registered for write? */
    protected final AtomicBoolean writeRegistered = new AtomicBoolean(false);

    /** Are we waiting to close? */
    protected boolean isClosing = false;

    /**
     * Create a new SocketWrapper
     *
     * @param channel Channel to Wrap.
     * @param owner ConnectedSocket that owns this.
     * @param key The selection key corresponding to the channel's registration
     */
    public SocketWrapper(final SocketChannel channel,
            final ConnectedSocket owner, final SelectionKey key) {
        this.key = key;

        myOwner = owner;
        mySocketChannel = channel;
        mySocket = channel.socket();
    }

    /**
     * Used to send a line of data to this socket.
     * This adds to the buffer.
     *
     * @param line Line to send
     */
    public final void sendLine(final String line) {
        if (outBuffer == null) {
            Logger.error("Null outBuffer -> " + line); return;
        }

        if (mySocketChannel == null) {
            Logger.error("Null mySocketChannel -> " + line);
            return;
        }

        if (myOwner == null) {
            Logger.error("Null myOwner -> " + line);
            return;
        }

        if (!mySocketChannel.isConnected()) {
            Logger.error("Trying to write to Disconnected SocketChannel -> " + line);
            myOwner.close();
            return;
        }

        synchronized(this) {
            if (isClosing) {
                return;
            }
        }

        synchronized (outBuffer) {
            outBuffer.append(line).append("\r\n");

            synchronized(writeRegistered) {
                if (!writeRegistered.get()) {
                    try {
                        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                        ConnectedSocketSelector.getConnectedSocketSelector().getSelector().wakeup();
                        writeRegistered.set(true);
                    } catch (CancelledKeyException ex) {
                        Logger.warning("Trying to write but key is cancelled -> " + line);
                        myOwner.close();
                    }
                }
            }
        }
    }


    /**
     * Used to get the ByteChannel for read and write operations
     *
     * @return ByteChannel to read/write to
     */
    protected ByteChannel getByteChannel() {
        if (myByteChannel == null) {
            return mySocketChannel;
        } else {
            return myByteChannel;
        }
    }

    /**
     * Used to get the SocketChannel we are wrapping
     *
     * @return SocketChannel we are wrapping
     */
    protected SocketChannel getSocketChannel() {
        return mySocketChannel;
    }

    /**
     * Close this SocketWrapper
     *
     * @throws IOException If there is a problem closing either of the ByteChannels
     */
    public void close() throws IOException {
        synchronized(this) { isClosing = true; }

        // Write any waiting data.
        // This has the potential to cause some kind of breakage that I can't
        // actualy remember right now related to writing at teh wrong time.
        // I don't think it matters because we are closing anyway and it only
        // affects the broken socket, but here be potenially bad breaking code
        // and this should be the first port of call for any break-on-close
        // related bugs!
        synchronized (outBuffer) {
            if (outBuffer.length() > 0) {
                ByteBuffer buf = ByteBuffer.wrap(outBuffer.toString().getBytes());
                outBuffer.setLength(0);
                try {
                    write(buf);
                } catch (IOException e) { }
            }
        }

        if (myByteChannel != null) {
            myByteChannel.close();
        }
        if (mySocketChannel.isOpen()) {
            mySocketChannel.close();
        }
    }

    /**
     * Write to this Socket
     *
     * @param data ByteBuffer to write
     * @return Number of bytes that were written to the channel.
     * @throws IOException If there is a problem writing to the channel.
     */
    protected int write(final ByteBuffer data) throws IOException {
        return getByteChannel().write(data);
    }

    /**
     * Read from this socket
     *
     * @param data ByteBuffer to read into
     * @return Number of bytes that were read from the channel.
     * @throws IOException If there is a problem reading from the channel.
     */
    protected int read(final ByteBuffer data) throws IOException {
        return getByteChannel().read(data);
    }

    /**
     * Get the remote SocketAddress for this socket
     *
     * @return Remote SocketAddress for this socket
     */
    public SocketAddress getRemoteSocketAddress() {
        return mySocket.getRemoteSocketAddress();
    }

    /**
     * Get the local SocketAddress for this socket
     *
     * @return Remote SocketAddress for this socket
     */
    public SocketAddress getLocalSocketAddress() {
        return mySocket.getLocalSocketAddress();
    }

    /**
     * Handles events from the socket.
     *
     * @param selKey SelectionKey from socket selector
     * @throws IOException If there is a problem processing the key
     */
    public final void processSelectionKey(final SelectionKey selKey) throws IOException {
        final SocketChannel channel = (SocketChannel) selKey.channel();

        if (channel != mySocketChannel) {
            // Not sure if there's any point in this...
            throw new IOException("Message for wrong channel.");
        }

        if (selKey.isValid() && selKey.isConnectable() && !channel.finishConnect()) {
            selKey.cancel();
        }

        if (selKey.isValid() && selKey.isReadable()) {
            // Clear the buffer and read bytes from socket
            buffer.clear();
            int numBytesRead = 0;
            do {
                buffer.clear();
                numBytesRead = read(buffer);
                if (numBytesRead == -1) {
                    if (selKey.isValid()) {
                        selKey.interestOps(0);
                        selKey.cancel();
                    }
                    Logger.info("Socket got closed.");
                    myOwner.close();
                    break;
                } else {
                    buffer.flip();
                    CharBuffer charBuffer = Charset.forName("UTF-8").newDecoder().decode(buffer);
                    char c;
                    for (int i = 0; i < charBuffer.limit(); ++i) {
                        c = charBuffer.get();
                        if (c == '\n') {
                            myOwner.processLine(lineBuffer.toString());
                            lineBuffer = new StringBuilder();
                        } else if (c != '\r') {
                            lineBuffer.append(c);
                        }
                    }
                }
            } while (numBytesRead != 0);
        } else if (selKey.isValid() && selKey.isWritable()) {
            ByteBuffer buf;
            synchronized (outBuffer) {
                if (outBuffer.length() > 0) {
                    buf = ByteBuffer.wrap(outBuffer.toString().getBytes());
                    outBuffer.setLength(0);
                } else {
                    selKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
                    writeRegistered.set(false);
                    return;
                }
            }

            try {
                write(buf);
            } catch (IOException e) {
                Logger.info("Socket has been closed.");
                myOwner.close();
            }
        }
    }
}
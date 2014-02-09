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

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Arrays;

import com.dfbnc.util.IRCLine;
import uk.org.dataforce.libs.logger.Logger;

/**
 * This defines a basic SocketWrapper
 */
public abstract class SocketWrapper {
    /** Used to hold incoming data. */
    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    /** Used to process incoming data. */
    private ByteBuffer lineBuffer = ByteBuffer.allocate(1024);

    /** Lines to be sent to the user go into this buffer. */
    protected final OutputBuffer outbuffer = new OutputBuffer();

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
    /** ByteBuffer used during write requests. */
    private ByteBuffer outputBytes = null;

    /** Are we waiting to close? */
    protected boolean isClosing = false;

    /** The charsets list used when trying to decode messages. */
    private final ArrayList<Charset> myCharsets = new ArrayList<>();

    /**
     * Create a new SocketWrapper
     *
     * @param channel Channel to Wrap.
     * @param owner ConnectedSocket that owns this.
     * @param key The selection key corresponding to the channel's registration
     */
    public SocketWrapper(final SocketChannel channel, final ConnectedSocket owner, final SelectionKey key) {
        this.key = key;

        myOwner = owner;
        mySocketChannel = channel;
        mySocket = channel.socket();
    }

    /**
     * Try to write the output buffer to the socket.
     *
     * If outputBytes is null, take all the data out of OutputBuffer and feed
     * it to the socket, if its not null we try to send the remaining data from
     * the last write attempt.
     *
     * If the socket accepted all the data (ie, its internal buffer didn't fill
     * up) then we set outputBytes back to null, otherwise we leave it around
     * until the next time we are called. (We return true, so OP_WRITE will
     * still be registered, so we should be called again soon.)
     *
     * @return False if no data was available to write, else true.
     * @throws IOException If there was a problem writing to the socket.
     */
    public boolean writeBuffer() throws IOException {
        synchronized (outbuffer) {
            if (outputBytes == null) {
                final String data = outbuffer.getAndReset();
                if (!data.isEmpty()) {
                    outputBytes = ByteBuffer.wrap(data.getBytes());
                }
            }
            if (outputBytes != null && outputBytes.remaining() > 0) {
                write(outputBytes);
                if (outputBytes.remaining() == 0) {
                    outputBytes = null;
                }
                return true;
            }

            return false;
        }
    }

    /**
     * Used to send a line of data to this socket.
     * This adds to the buffer.
     *
     * @param line Line to send
     */
    public final void sendLine(final String line) {
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
            myOwner.closeSocket("SocketChannel disconnected.");
            return;
        }

        synchronized(this) {
            if (isClosing) {
                return;
            }
        }

        outbuffer.addData(line, "\r\n");

        try {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            SocketSelector.getConnectedSocketSelector().getSelector().wakeup();
        } catch (CancelledKeyException ex) {
            Logger.warning("Trying to write but key is cancelled -> " + line);
            myOwner.closeSocket("Write Key Cancelled.");
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
     * Check if this socket is still connected
     *
     * @return True if the SocketChannel we are wrapping is connected.
     */
    public boolean isConnected() {
        return mySocketChannel.isConnected();
    }

    /**
     * Close this SocketWrapper
     *
     * @throws IOException If there is a problem closing either of the ByteChannels
     */
    public void close() throws IOException {
        synchronized(this) { isClosing = true; }

        // Write any waiting data.
        //
        // This has the potential to cause some kind of breakage that I can't
        // actualy remember right now related to writing at the wrong time.
        // I don't think it matters because we are closing anyway and it only
        // affects the broken socket, but here be potenially bad breaking code
        // and this should be the first port of call for any break-on-close
        // related bugs!
        //
        // 2011-10-02 DF: The above was written a long time ago, I don't think
        // it really applies anymore since some things were rewritten, I
        // certainly haven't seen any recent break-on-close bugs!
       try {
            writeBuffer();
        } catch (IOException e) { }

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
     * Get the charset currently used by this socket.
     *
     * @return The charset in use by this socket.
     */
    public Charset getCharset() {
        synchronized (myCharsets) {
            return myCharsets.get(0);
        }
    }

    /**
     * Set the charset currently used by this socket.
     *
     * @param cs Charset to use as a string.
     * @return True if the charset was set.
     */
    public boolean setCharset(final String cs) {
        try {
            setCharset(Charset.forName(cs));
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Set the charset currently used by this socket.
     *
     * @param cs Charset to use.
     */
    public void setCharset(final Charset cs) {
        synchronized (myCharsets) {
            myCharsets.clear();
            // First add the current socket charset as the first to try
            myCharsets.add(cs);
            Logger.debug4("Added charset to list: "+cs.displayName());
            // Now add the fallbacks.
            for (String charset : Arrays.asList("UTF-8", "windows-1252", "ISO-8859-1")) {
                try {
                    final Charset c = Charset.forName(charset);
                    if (!myCharsets.contains(c)) {
                        myCharsets.add(c);
                        Logger.debug4("Added charset to list: "+c.displayName());
                    }
                } catch (final Exception e) { /* Do nothing. */ }
            }
        }
    }

    /**
     * Try and decode the incoming buffer using a socket-specific charset,
     * followed by some fallback charsets before throwing an exception.
     * This is not ideal, as something may well decode with one of the charsets
     * but not display correctly. However, its better than throwing an exception
     * and disconnecting the user.
     *
     * @param buffer ByteBuffer to try and decode
     * @return CharBuffer created by decoding buffer
     * @throws CharacterCodingException if we were unable to convert the charset
     */
    private CharBuffer getCharBuffer(final ByteBuffer buffer) throws CharacterCodingException {
        CharBuffer charBuffer = null;

        CharacterCodingException firstException = null;
        synchronized (myCharsets) {
            if (myCharsets.isEmpty()) {
                Logger.debug4("No charsets set, using default");
                setCharset("UTF-8");
            }
            Logger.debug4("My Charsets: "+myCharsets);
            buffer.mark();
            for (Charset c : myCharsets) {
                try {
                    Logger.debug4("Charset: " + c);
                    buffer.reset();
                    charBuffer = c.newDecoder().decode(buffer);
                    return charBuffer;
                } catch (final CharacterCodingException cce) {
                    if (firstException != null) {
                        firstException = cce;
                    }
                }
            }
        }

        // If we get here, no charsets worked, return the first exception as
        // it was the one against the socket charset.
        throw firstException;
    }

    /**
     * Handles events from the socket.
     *
     * @param selKey SelectionKey from socket selector
     * @throws IOException If there is a problem processing the key
     */
    public final void handleSelectionKey(final SelectionKey selKey) throws IOException {
        final SocketChannel channel = (SocketChannel) selKey.channel();

        if (channel != mySocketChannel) {
            // Not sure if there's any point in this...
            throw new IOException("Message for wrong channel.");
        }

        if (selKey.isValid() && selKey.isConnectable() && !channel.finishConnect()) {
            selKey.cancel();
        }

        if (selKey.isValid() && selKey.isReadable()) {
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
                    myOwner.closeSocket("EOF from client.");
                    break;
                } else if (numBytesRead != 0) {
                    buffer.flip();

                    byte b;
                    for (int i = 0; i < buffer.limit(); ++i) {
                        b = buffer.get();
                        if (b == '\n') {
                            lineBuffer.flip();
                            myOwner.processLine(getCharBuffer(lineBuffer).toString());
                            lineBuffer = ByteBuffer.allocate(1024);
                        } else if (b != '\r') {
                            lineBuffer.put(b);
                        }
                    }
                }
            } while (numBytesRead != 0);
        } else if (selKey.isValid() && selKey.isWritable()) {
            try {
                final boolean wroteData = writeBuffer();

                // If we didn't write any data, then the buffer was empty, so
                // we need to switch back to read mode.
                if (!wroteData) {
                    try {
                        selKey.interestOps(SelectionKey.OP_READ);
                    } catch (final CancelledKeyException cke) {
                        Logger.warning("Trying to switch back to read but key is cancelled");
                        myOwner.closeSocket("Read key cancelled.");
                    }
                    return;
                }
            } catch (final IOException ioe) {
                Logger.info("Socket has been closed.");
                myOwner.closeSocket("IOException on socket: " + ioe);
            }
        }
    }

    /**
     * Handle an IOException from the socket.
     *
     * @param selKey SelectionKey we were given.
     * @param ioe Exception to handle.
     * @return Boolean, true if socket should be closed.
     */
    public boolean handleIOException(final IOException ioe) {
        return true;
    }
}
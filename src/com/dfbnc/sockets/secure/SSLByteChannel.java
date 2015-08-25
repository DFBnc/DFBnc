/*
 * This class is given to the Public Domain. Do what you want with it.
 * This software comes with no guarantees or warranties.
 *
 * This is a modified version of David Crosson's Public Domain implementation
 * from http://perso.wanadoo.fr/reuse/sslbytechannel/
 *
 * Specific Changes by Shane 'Dataforce' Mc Cormack (dataforce@dataforce.org.uk)
 *  - unwrap() will now throw a ClosedChannelException() if the channel has
 *    been closed when reading, rather than just sitting in a loop forever.
 *  - Fix 100% CPU during long or repeated unwrap() calls;
 *  - Fix a possible infinite loop in sslloop();
 *
 * In cases where "Public Domain" is not acceptable (either legally or by way of
 * local policy) or where such a license could expose the author(s) to extra
 * obligations, warranties or responsibilities, then this software should be
 * considered to be licensed as follows:
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
package com.dfbnc.sockets.secure;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * Wrapper for ByteChannel to read/write SSL Sockets.
 *
 * @version 1.0.3
 */
public class SSLByteChannel implements ByteChannel {
    /** The raw channel we are wrapping */
    private final ByteChannel myChannel;
    /** SSL Engine for this channel */
    private SSLEngine myEngine;

    /** Used to store unwraped incomming data */
    private final ByteBuffer inAppData;
    /** Used to store unwraped outgoing data */
    private final ByteBuffer outAppData;

    /** Used to store wraped incoming data */
    private final ByteBuffer inNetData;
    /** Used to store wraped outgoing data */
    private final ByteBuffer outNetData;

    /** Prevents trying to close twice. */
    private boolean socketOpen = true;

    /**
     * Create a new SSLByteChannel by wrapping an old one.
     *
     * @param channel Channel to wrap
     * @param engine SSLEngine to use
     */
    public SSLByteChannel(final ByteChannel channel, final SSLEngine engine) {
        myChannel = channel;
        myEngine = engine;

        // Create the ByteBuffers
        SSLSession session = myEngine.getSession();
        inAppData  = ByteBuffer.allocate(session.getApplicationBufferSize());
        outAppData = ByteBuffer.allocate(session.getApplicationBufferSize());

        inNetData  = ByteBuffer.allocate(session.getPacketBufferSize());
        outNetData = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    /**
     * This cleanly ends the SSL Session, and then closes the channel
     *
     * @throws IOException If there is any problem closing the channel
     */
    @Override
    public void close() throws IOException {
        if (socketOpen) {
            try {
                myEngine.closeOutbound();
                try {
                    sslLoop(wrap());
                } catch (ClosedChannelException cce) { /* Duh.. ? */ }
                myChannel.close();
            } finally {
                socketOpen = false;
            }
        }
    }

    /**
     * Check if this ByteChannel is still open.
     *
     * @return True if the Channel has not yet been closed.
     */
    @Override
    public boolean isOpen() {
        return socketOpen;
    }

    /**
     * Read bytes from the socket into the given buffer.
     *
     * @param buffer Buffer to read bytes into
     * @return Number of bytes read, or -1 if the socket was closed.
     * @throws IOException If there was a problem reading from the ByteChannel
     */
    @Override
    public int read(final ByteBuffer buffer) throws IOException {
        // Try and get the data.
        if (isOpen()) {
            try {
                final SSLEngineResult r = sslLoop(unwrap());
                if (r != null && (r.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW && r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
                    throw new SSLException("Unrecognized SSL data, plaintext connection?");
                }
            } catch (final ClosedChannelException e) {
                close();
            }
        }

        // Add the data to the buffer
        inAppData.flip();
        int posBefore = buffer.position();
        buffer.put(inAppData);
        int posAfter = buffer.position();
        inAppData.compact();

        // Return status
        if (posAfter - posBefore > 0) {
            return posAfter - posBefore;
        } else if (isOpen()) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * Write bytes to the socket from the given buffer.
     *
     * @param buffer Buffer to get bytes from
     * @return Number of bytes written.
     * @throws IOException If there was a problem writing to the ByteChannel
     */
    @Override
    public int write(final ByteBuffer buffer) throws IOException {
        if (!isOpen()) { return 0; }

        int posBefore = buffer.position();
        if (buffer.remaining() < outAppData.remaining()) {
            outAppData.put(buffer);
        } else {
            while (buffer.hasRemaining() && outAppData.hasRemaining()) {
             outAppData.put(buffer.get());
            }
        }
        int posAfter = buffer.position();

        if (isOpen()) {
            try {
                while(true) {
                    final SSLEngineResult r = sslLoop(wrap());
                    if (r.bytesConsumed() == 0 && r.bytesProduced() == 0) {
                        break;
                    }
                }
            } catch (SSLException se) {
                throw new IOException("Problem with SSL: "+se.getMessage(), se);
            } catch(ClosedChannelException e) {
                close();
            }
        }

        return posAfter - posBefore;
    }

    /**
     * Unwrap data from inNetData to inAppData
     *
     * @return Result of unwrap() operation on SSLEngine
     * @throws IOException If there was problems manipulating the ByteBuffers
     * @throws SSLException If there was a problem processing the SSL Stream
     */
    private WrapResult unwrap() throws IOException, SSLException {
        // Read in as much data as we can
        int count;
        int total = 0;
        do {
            count = myChannel.read(inNetData);
            total += count;
        } while (count > 0);

        if (count < 0) { throw new ClosedChannelException(); }

        // Unwrap it into the buffer
        inNetData.flip();
        final SSLEngineResult ser;
        try {
            ser = myEngine.unwrap(inNetData, inAppData);
        } catch (final SSLException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new SSLException(ex);
        }
        inNetData.compact();

        return new WrapResult(ser, total, WrapResultType.UNWRAP);
    }

    /**
     * Wrap data from inAppData into inNetData and write it out to the socket.
     *
     * @return Result of wrap() operation on SSLEngine
     * @throws IOException If there was problems manipulating the ByteBuffers
     * @throws SSLException If there was a problem processing the SSL Stream
     */
    private WrapResult wrap() throws IOException, SSLException {
        // Wrap the data
        outAppData.flip();
        final SSLEngineResult ser;
        try {
            ser = myEngine.wrap(outAppData,  outNetData);
        } catch (final SSLException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new SSLException(ex);
        }
        outAppData.compact();

        // Write it out
        outNetData.flip();
        int total = 0;
        while (outNetData.hasRemaining()) {
            total += myChannel.write(outNetData);
        }
        outNetData.compact();

        return new WrapResult(ser, total, WrapResultType.WRAP);
    }

    /**
     * This handles handshaking if needed
     *
     * @param inputResult SSLEngineResult to work on (This lets us know if we
     *                    need to handshake again or not.)
     * @return Last SSLEngineResult from tasks
     * @throws SSLException If there is a problem with the SSL Tasks
     * @throws IOException If there is a problem with the socket
     */
    private SSLEngineResult sslLoop(final WrapResult inputResult) throws SSLException, IOException {
        if (inputResult == null) { return null; }

        WrapResult wrapResult = inputResult;

        // Handshake if needed.
        HandshakeStatus hsStatus = wrapResult.ser.getHandshakeStatus();
        boolean isTask = (hsStatus == HandshakeStatus.NEED_TASK);
        while (hsStatus != HandshakeStatus.FINISHED && hsStatus != HandshakeStatus.NOT_HANDSHAKING) {
            hsStatus = wrapResult.ser.getHandshakeStatus();
            switch (hsStatus) {
                case NEED_TASK:
                    // Sometimes the sslEngine decides it needs todo a potentially blocking
                    // task. It uses NEED_TASK to allow tasks to be run in a different thread
                    // if desired.
                    Runnable task;
                    while ((task = myEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    wrapResult = (wrapResult.type == WrapResultType.WRAP) ? wrap() : unwrap();
                    break;
                case NEED_WRAP:
                    wrapResult = wrap();
                    break;
                case NEED_UNWRAP:
                    wrapResult = unwrap();
                    break;
            }

            if (!isTask && hsStatus == HandshakeStatus.NEED_UNWRAP && wrapResult.bytes == 0) {
                break;
            }

            if (wrapResult.ser.getStatus() == SSLEngineResult.Status.CLOSED) {
                break;
            }
        }

        // Check if the socket was closed.
        switch(wrapResult.ser.getStatus()) {
            case CLOSED:
                try {
                    myChannel.close();
                } finally {
                    socketOpen = false;
                }
                break;
        }

        return wrapResult.ser;
    }

    private enum WrapResultType { WRAP, UNWRAP; }
    private class WrapResult {
        public final SSLEngineResult ser;
        public final int bytes;
        public final WrapResultType type;

        public WrapResult(final SSLEngineResult ser, final int bytes, final WrapResultType type) {
            this.ser = ser;
            this.bytes = bytes;
            this.type = type;
        }
    }
}
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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * This defines a Plain (non-ssl) Socket.
 * SocketWrapper handles everything for this.
 */
public class PlainSocket extends SocketWrapper {

    /**
     * Create a new PlainSocket
     *
     * @param channel Channel to Wrap.
     * @param owner ConnectedSocket that owns this.
     * @param key The selection key corresponding to the channel's registration
     * @throws IOException If there is a problem creating the socket
     */
    public PlainSocket (final SocketChannel channel, final ConnectedSocket owner, final SelectionKey key) throws IOException {
        super(channel, owner, key);
    }
}
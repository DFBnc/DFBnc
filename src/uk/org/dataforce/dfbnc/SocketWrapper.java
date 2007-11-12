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

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;

/**
 * This defines a basic SocketWrapper 
 */
public abstract class SocketWrapper {
	/** Used to process incomming data. */
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
	/** Used to process outgoing data. */
	private StringBuilder lineBuffer = new StringBuilder();
	/** Lines to be sent to the user go into this buffer. */
	protected StringBuffer outBuffer = new StringBuffer();

	/** The ConnectedSocket that owns this Wrapper */
	protected ConnectedSocket myOwner;

	/** My socket channel. */
	protected final SocketChannel mySocketChannel;
	/** My socket channel socket. */
	protected final Socket mySocket;
	/** My Byte Channel. */
	protected ByteChannel myByteChannel;
	
	/**
	 * Used to send a line of data to this socket.
	 * This adds to the buffer.
	 *
	 * @param line Line to send
	 */
	public final void sendLine(final String line) {
		synchronized (outBuffer) {
			outBuffer.append(line+"\r\n");
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
	 * Create a new SocketWrapper
	 *
	 * @param channel Channel to Wrap.
	 * @param owner ConnectedSocket that owns this.
	 */
	public SocketWrapper (final SocketChannel channel, final ConnectedSocket owner) {
		myOwner = owner;
		mySocketChannel = channel;
		mySocket = channel.socket();
	}
	
	/**
	 * Close this SocketWrapper
	 * 
	 * @throws IOException If there is a problem closing either of the ByteChannels
	 */
	public void close() throws IOException {
		if (myByteChannel != null && myByteChannel != mySocketChannel) {
			myByteChannel.close();
		}
		mySocketChannel.close();
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
		if (selKey.isValid() && selKey.isConnectable()) {
			SocketChannel sChannel = (SocketChannel)selKey.channel();
			if (sChannel != mySocketChannel) { throw new IOException("Message for wrong channel."); }
			
			boolean success = sChannel.finishConnect();
			if (!success) {
				selKey.cancel();
			}
		}
		if (selKey.isValid() && selKey.isReadable()) {
			SocketChannel sChannel = (SocketChannel)selKey.channel();
			if (sChannel != mySocketChannel) { throw new IOException("Message for wrong channel."); }
		
			// Clear the buffer and read bytes from socket
			buffer.clear();
			int numBytesRead = 0;
			do {
				buffer.clear();
				numBytesRead = read(buffer);
				if (numBytesRead == -1) {
					sChannel.close();
					myOwner.socketClosed(true);
				} else {
					buffer.flip();
					CharBuffer charBuffer = Charset.forName("us-ascii").newDecoder().decode(buffer);
					char c;
					for (int i = 0; i < charBuffer.limit(); ++i) {
						c = charBuffer.get();
						if (c == '\n') {
							try {
								myOwner.processLine(lineBuffer.toString());
							} catch (Exception e) { }
							lineBuffer = new StringBuilder();
						} else if (c != '\r') {
							lineBuffer.append(c);
						}
					}
				}
			} while (numBytesRead != 0);
		} else if (selKey.isValid() && selKey.isWritable()) {
			SocketChannel sChannel = (SocketChannel)selKey.channel();
			if (sChannel != mySocketChannel) { throw new IOException("Message for wrong channel."); }
			
			ByteBuffer buf;
			synchronized (outBuffer) {
				if (outBuffer.length() > 0) {
					buf = ByteBuffer.wrap(outBuffer.toString().getBytes());
					outBuffer = new StringBuffer();
				} else {
					return;
				}
			}
			try {
				write(buf);
			} catch (IOException e) {
				myOwner.socketClosed(false);
			}
		}
	}
}
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

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.io.IOException;

/**
 * This is responsible for taking incomming data, and separating it
  * into "\n" separated lines.
 */
public abstract class ConnectedSocket implements Runnable {
	/** Used to monitor the socket. */
	private Selector selector = null;
	/** Thread to run the listen socket under */	
	private Thread myThread = new Thread(this);
	/** Used to process incomming data. */
	private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
	/** Used to process incomming data. */
	private StringBuilder lineBuffer = new StringBuilder();

	/**
	 * Create a new ConnectedSocket.
	 *
	 * @param sChannel Socket to control
	 * @param threadName Name to call thread that this socket runs under.
	 */
	protected ConnectedSocket(SocketChannel sChannel, String threadName) throws IOException {
		selector = Selector.open();
		
		sChannel.configureBlocking(false);
		sChannel.register(selector, sChannel.validOps());
		
		myThread.setName(threadName);
		myThread.start();
	}
		
	/**
	 * Used to actually do stuff.
	 */
	public final void run() {	
		while (true) {
			try {
				selector.select();
			} catch (IOException e) {
				break;
			}

			Iterator it = selector.selectedKeys().iterator();
			
			while (it.hasNext()) {
				SelectionKey selKey = (SelectionKey)it.next();
				it.remove();

				try {
					processSelectionKey(selKey);
				} catch (IOException e) {
					selKey.cancel();
					break;
				}
			}
		}
		this.socketClosed();
	}
	
	/**
	 * Handles events from the socket.
	 *
	 * @param selKey SelectionKey from socket selector
	 */
	private final void processSelectionKey(SelectionKey selKey) throws IOException {
		if (selKey.isValid() && selKey.isConnectable()) {
			SocketChannel sChannel = (SocketChannel)selKey.channel();
			
			boolean success = sChannel.finishConnect();
			if (!success) {
				selKey.cancel();
			}
		}
		if (selKey.isValid() && selKey.isReadable()) {
			SocketChannel sChannel = (SocketChannel)selKey.channel();
		
			// Clear the buffer and read bytes from socket
			buffer.clear();
			int numBytesRead = sChannel.read(buffer);
	
			if (numBytesRead == -1) {
				sChannel.close();
			} else {
				buffer.flip();
				CharBuffer charBuffer = Charset.forName("us-ascii").newDecoder().decode(buffer);
				char c;
				for (int i = 0; i < charBuffer.limit(); ++i) {
					c = charBuffer.get();
					if (c == '\n') {
						try {
							processLine(lineBuffer.toString());
						} catch (Exception e) { }
						lineBuffer = new StringBuilder();
					} else if (c != '\r') {
						lineBuffer.append(c);
					}
				}
			}
		}
	}
	
	/**
	 * Process a line of data.
	 *
	 * @param line Line to handle
	 */
	abstract void processLine(final String line);
	
	
	/**
	 * Action to take when socket is closed.
	 */
	protected void socketClosed() { }
}

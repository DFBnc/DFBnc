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

import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.io.IOException;


import uk.org.dataforce.libs.logger.Logger;

/**
 * This is responsible for taking incomming data, and separating it
  * into "\n" separated lines.
  *
  * This can be run in 2 ways, multi-thread and single-thread.
  * Multi-thread mode uses individual selectors for each socket
  * Single-thread mode uses a single selector for all sockets.
  * The mode in use is defined in the config, Single-Thread is the default 
 */
public abstract class ConnectedSocket implements Runnable {
	/** Used to monitor the socket. */
	private Selector selector = null;
	/** Thread to run the listen socket under */	
	protected Thread myThread = new Thread(this);
	/** SocketWrapper, used to allow for SSL Sockets */
	protected final SocketWrapper mySocketWrapper;
	/** String to identify socket by */
	private String socketID = "ConnectedSocket";
	/** Has this socket been closed? */
	private boolean isClosed = false;
	/** Are we an SSL Socket? */
	protected final boolean isSSL;

	/**
	 * Create a new ConnectedSocket.
	 *
	 * @param sChannel Socket to control
	 * @param idstring Name to call this socket.
	 * @param fromSSL Did this come from an SSL ListenSocket?
	 * @throws IOException If there is a problem creating Socket
	 */
	protected ConnectedSocket(SocketChannel sChannel, String idstring, final boolean fromSSL) throws IOException {
		isSSL = fromSSL;
		
		sChannel.configureBlocking(false);
		if (!isSingleThread()) {
			selector = Selector.open();
			sChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
		}
		
		if (isSSL) {
			mySocketWrapper = new SecureSocket(sChannel, this);
		} else {
			mySocketWrapper = new PlainSocket(sChannel, this);
		}
		
		setSocketID(socketID);
		if (isSingleThread()) {
			ConnectedSocketSelector.getConnectedSocketSelector().registerSocket(sChannel, this);
		} else {
			myThread.start();
		}
	}
		
	/**
	 * Check if we are in single thread or multi thread mode.
	 *
	 * @return The negative of the value of config option general.socket.multithread,
	 *         or true if the option is undefined.
	 */
	public final boolean isSingleThread() {
		return !Config.getBoolOption("general", "socket.multithread", false);
	}
		
	/**
	 * Used to close this socket.
	 */
	public final void close() {
		if (isClosed) { return; }
		Logger.info("Connected Socket closing ("+socketID+")");
		isClosed = true;

		if (!isSingleThread()) {
			// Kill the thread
			Thread tmp = myThread;
			myThread = null;
			if (tmp != null) { tmp.interrupt(); }
		}
		
		// Close the actual socket
		try {
			if (isSingleThread()) {
				ConnectedSocketSelector.getConnectedSocketSelector().unregisterSocket(mySocketWrapper.getSocketChannel());
			}
			mySocketWrapper.close();
		} catch (IOException e) { }
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
		return mySocketWrapper;
	}
	
	/**
	 * Set this Sockets ID
	 *
	 * @param idstring New ID String for this socket
	 */
	public void setSocketID (final String idstring) {
		socketID = idstring;
		myThread.setName(socketID);
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
	 * Used to actually do stuff.
	 */
	@Override
	public final void run() {
		if (isSingleThread()) { return; }
		
		while (myThread == Thread.currentThread()) {
			try {
				int res = selector.select();
				if (res == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) { }
				}
			} catch (IOException e) {
				break;
			}
			
			Iterator it = selector.selectedKeys().iterator();
			
			while (it.hasNext()) {
				SelectionKey selKey = (SelectionKey)it.next();
				it.remove();

				try {
					mySocketWrapper.processSelectionKey(selKey);
				} catch (IOException e) {
					selKey.cancel();
					break;
				}
			}
		}
	}
		
	/**
	 * Get the selector in use by this Socket
	 *
	 * @return The selector in use by this Socket
	 */
	public Selector getSelector() {
		if (isSingleThread()) {
			return ConnectedSocketSelector.getConnectedSocketSelector().getSelector();
		} else {
			return selector;
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
	public void socketOpened() { }
	
	/**
	 * Action to take when socket is closed.
	 *
	 * @param userRequested True if socket was closed by the user, false otherwise
	 */
	protected void socketClosed(final boolean userRequested) { }
}

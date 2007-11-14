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
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.HashMap;
import java.io.IOException;


import uk.org.dataforce.logger.Logger;

/**
 * This is responsible for handling the selector for all Sockets when in
 * single-thread mode.
 * This is not used at all in multi-thread mode.
 */
public class ConnectedSocketSelector implements Runnable {
	/** Used to monitor the sockets. */
	private Selector selector = null;
	/** Thread to run the selector under */	
	private Thread myThread = new Thread(this);
	/** Hashmap containing known sockets */
	private HashMap<SocketChannel, ConnectedSocket> knownSockets = new HashMap<SocketChannel, ConnectedSocket>();
	/** Instance of ConnectedSocketSelector in use */
	private static ConnectedSocketSelector myConnectedSocketSelector = null;
	
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
	 */
	public void registerSocket(final SocketChannel channel, final ConnectedSocket socket) throws ClosedChannelException {
		synchronized (knownSockets) {
			if (!knownSockets.containsKey(channel)) {
				selector.wakeup();
				channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
				knownSockets.put(channel, socket);
			}
		}
	}
	
	/**
	 * Unregister a ConnectedSocket
	 *
	 * @param channel SocketChannel to unregister 
	 */
	public void unregisterSocket(final SocketChannel channel) {
		synchronized (knownSockets) {
			if (knownSockets.containsKey(channel)) {
				knownSockets.remove(channel);
			}
		}
	}
		
	/**
	 * Used to actually do stuff.
	 */
	@Override
	public final void run() {
		while (myThread == Thread.currentThread()) {
			try {
				int res = selector.select();
				if (res == 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) { }
				}
			} catch (Exception e) {
				break;
			}
			
			Iterator it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey selKey = (SelectionKey)it.next();
				it.remove();

				try {
					synchronized (knownSockets) {
						SocketChannel sChannel = (SocketChannel)selKey.channel();
						ConnectedSocket cSocket = knownSockets.get(sChannel);
						if (cSocket != null) {
							cSocket.getSocketWrapper().processSelectionKey(selKey);
						}
					}
				} catch (Exception e) {
					selKey.cancel();
					break;
				}
			}
		}
	}
}

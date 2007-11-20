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

import uk.org.dataforce.logger.Logger;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.io.IOException;

/**
 * The ServerSocket for the application.
 * This handles and redirects all incomming connections to UserSockets
 */
public class ListenSocket implements Runnable {
	/** Used to monitor the socket. */
	private Selector selector = null;
	/** My ServerSocketChannel. */
	private ServerSocketChannel ssChannel = ServerSocketChannel.open();
	/** Thread to run the listen socket under */	
	private volatile Thread myThread = new Thread(this);
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
	 * Create a new ListenSocket.
	 *
	 * @param host Hostname to listen on
	 * @param port Hostname to listen on
	 * @throws IOException if there is problems with the sockets.
	 */
	public ListenSocket(final String host, final int port) throws IOException {
		setupSocket(host, port, false);
	}
	
	/**
	 * Create a new ListenSocket.
	 *
	 * @param host Hostname to listen on
	 * @param port Hostname to listen on
	 * @param ssl True/false for ssl
	 * @throws IOException if there is problems with the sockets.
	 */
	public ListenSocket(final String host, final int port, final boolean ssl) throws IOException {
		setupSocket(host, port, ssl);
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
		if (ssl) {
			try { SecureSocket.getSSLContext(); }
			catch (Exception e) {
				Logger.error("Failed to open SSL Socket '"+host+":"+port+"'");
				Logger.error("Reason: "+e.getMessage());
			}
			throw new IOException("Unable to use SSL");
		}
		selector = Selector.open();
		
		ssChannel.configureBlocking(false);
		ssChannel.socket().bind(new InetSocketAddress(host, port));
		ssChannel.register(selector, SelectionKey.OP_ACCEPT);
		myThread.setName("[ListenSocket "+host+":"+port+"]");
		myThread.start();
		Logger.info("Listen Socket Opened: "+host+":"+port);
	}
		
	/**
	 * Close the socket
	 */
	public synchronized void close() {
		Logger.info("Listen Socket closing ("+myThread.getName()+")");

		// Kill the thread
		Thread tmp = myThread;
		myThread = null;
		if (tmp != null) { tmp.interrupt(); }
		
		// Close the actual socket
		try {
			ssChannel.socket().close();
		} catch (IOException e) { }
	}
		
	/**
	 * Used to actually do stuff!
	 */
	@Override
	public void run() {
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

				if (selKey.isAcceptable()) {
					ServerSocketChannel selChannel = (ServerSocketChannel)selKey.channel();
					
					try {
						SocketChannel sChannel = selChannel.accept();
						if (sChannel != null) {
							Logger.info("Accepting new socket.");
							UserSocket userSocket = new UserSocket(sChannel, isSSL);
							userSocket.socketOpened();
						}
					} catch (IOException e) {
						Logger.error("Unable to open UserSocket: "+e.getMessage());
					}
				}
			}
		}
	}
}

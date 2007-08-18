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
import java.util.Hashtable;

/**
 * The ServerSocket for the application.
 * This handles and redirects all incomming connections to UserSockets
 */
public class ListenSocket implements Runnable {
	/** Used to monitor the socket. */
	private Selector selector = null;
	/** Thread to run the listen socket under */	
	private Thread myThread = new Thread(this);

	/**
	 * Create a new ListenSocket.
	 *
	 * @param host Hostname to listen on
	 * @param port Hostname to listen on
	 * @throws IOException if there is problems with the sockets.
	 */
	public ListenSocket(String host, int port) throws IOException {
		selector = Selector.open();
		
		ServerSocketChannel ssChannel = ServerSocketChannel.open();
		
		ssChannel.configureBlocking(false);
		ssChannel.socket().bind(new InetSocketAddress(host, port));
		ssChannel.register(selector, SelectionKey.OP_ACCEPT);
		myThread.setName("[ListenSocket "+host+":"+port+"]");
		myThread.start();
		Logger.info("Listen Socket Opened: "+host+":"+port);
	}
		
	/**
	 * Used to actually do stuff!
	 */
	public void run() {	
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

				if (selKey.isAcceptable()) {
					ServerSocketChannel ssChannel = (ServerSocketChannel)selKey.channel();
					
					try {
						SocketChannel sChannel = ssChannel.accept();
						if (sChannel != null) {
							UserSocket userSocket = new UserSocket(sChannel);
							userSocket.socketOpened();
						}
					} catch (IOException e) { }
				}
			}
		}
	}
}

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
import java.io.IOException;
import java.util.Hashtable;

/**
 * This socket handles actual clients connected to the bnc.
 */
public class UserSocket extends ConnectedSocket {
	/** Known sockets are referenced in this hashtable. */
	private static Hashtable<String,UserSocket> knownSockets = new Hashtable<String,UserSocket>();
	
	/** This sockets ID in the hashtable. */
	private final String myID;

	/** This sockets info. */
	private final String myInfo;
	
	/**
	 * Create a new UserSocket.
	 *
	 * @param sChannel Socket to control
	 * @param threadName Name to call thread that this socket runs under.
	 */
	public UserSocket(SocketChannel sChannel) throws IOException {
		super(sChannel, "[UserSocket "+sChannel+"]");
		synchronized (knownSockets) {
			String tempid = this.toString();
			while (knownSockets.containsKey(tempid)) {
				tempid = this.toString()+"-"+Math.random();
			}
			myID = tempid;
			knownSockets.put(myID, this);
		}
		// myInfo = mySocket.getRemoteSocketAddress()+" ("+mySocket.getLocalSocketAddress()+") ["+myID+"]";
		// Do this rather than above because we want to enclose the addresses in []
		InetSocketAddress address = (InetSocketAddress)mySocket.getRemoteSocketAddress();
		String remoteInfo = "["+address.getAddress()+"]:"+address.getPort();
		address = (InetSocketAddress)mySocket.getLocalSocketAddress();
		String localInfo = "["+address.getAddress()+"]:"+address.getPort();
		myInfo = remoteInfo+" ("+localInfo+") ["+myID+"]";
		
		Logger.info("User Connected: "+myInfo);
	}
	
	/**
	 * Action to take when socket is opened and ready.
	 */
	public void socketOpened() {
		sendLine("NOTICE AUTH :- Welcome to DFBnc ("+DFBnc.VERSION+")");
		close();
	}
	
	/**
	 * Action to take when socket is closed.
	 *
	 * @param userRequested True if socket was closed by the user, false otherwise
	 */
	protected void socketClosed(final boolean userRequested) {
		knownSockets.remove(myID);
		Logger.info("User Disconnected: "+myInfo);
	}
	
	/**
	 * Process a line of data.
	 *
	 * @param line Line to handle
	 */
	protected void processLine(final String line) {
		// Woo!
	}
}

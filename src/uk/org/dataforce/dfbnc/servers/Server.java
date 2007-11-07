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
package uk.org.dataforce.dfbnc.servers;

import uk.org.dataforce.dfbnc.UserSocket;

/**
 * This file represents a Server
 */
public abstract class Server {
	/** Reference to the ServerManager in charge of this Server. */
	protected ServerManager myManager;

	/**
	 * Create a new instance of the Server Object.
	 *
	 * @param manager ServerManager that is in charge of this Server
	 */
	protected Server(final ServerManager manager) {
		this.myManager = manager;
	}
	
	/**
	 * Get the name for this Server.
	 * @return the name of this Server
	 */
	public final String getName() {
		final Package thisPackage = this.getClass().getPackage();
		int packageLength = 0;
		if (thisPackage != null) {
			packageLength = thisPackage.getName().length() + 1;
		}
		return this.getClass().getName().substring(packageLength);
	}
	
	/**
	 * Get the name for this Server in lowercase.
	 * @return lower case name of this Server
	 */
	public final String getLowerName() {
		return this.getName().toLowerCase();
	}
	
	/**
	 * Get the name for this Server.
	 * @return the name of this Server
	 */
	public final String toString() { return this.getName(); }
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo() { return "$Id: Server.java 1320 2007-05-21 09:53:01Z ShaneMcC $"; }	
}
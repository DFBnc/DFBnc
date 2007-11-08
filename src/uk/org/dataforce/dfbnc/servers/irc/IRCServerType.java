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
package uk.org.dataforce.dfbnc.servers.irc;

import uk.org.dataforce.dfbnc.servers.ServerType;
import uk.org.dataforce.dfbnc.servers.ServerTypeManager;
import uk.org.dataforce.dfbnc.Account;

/**
 * This file gives the ability to connect to an IRC Server
 */
public class IRCServerType extends ServerType {
	/**
	 * Create a new instance of the ServerType Object
	 *
	 * @param manager ServerTypeManager that is in charge of this ServerType
	 */
	public IRCServerType (final ServerTypeManager manager) { super(manager); }
	
	/**
	 * Get the Description for this ServerType
	 *
	 * @return lower case name of this ServerType
	 */
	public String getDescription() {
		return "This allows connecting to an IRC Server";
	}
	
	/**
	 * Called when this ServerType is activated
	 *
	 * @param account Account that activated the servertype
	 */
	public void activate(final Account account) {
	}
	
	/**
	 * Called when this ServerType is deactivated
	 *
	 * @param account Account that deactivated the servertype
	 */
	public void deactivate(final Account account) {
	}
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }	
}
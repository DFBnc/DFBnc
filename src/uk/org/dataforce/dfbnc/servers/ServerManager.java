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

import uk.org.dataforce.logger.Logger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * DFBNC Server Manager.
 * Manages adding/removing/creating servers.
 *
 * @author Shane Mc Cormack
 * @version $Id: ServerManager.java 1360 2007-05-25 19:12:05Z ShaneMcC $
 */
public final class ServerManager {
	/** Hashtable used to store the different types of Server known. */
	private Hashtable<String,Server> knownServers = new Hashtable<String,Server>();

	/**
	 * Constructor to create a ServerManager
	 */
	public ServerManager() { }
	
	/**
	 * Initialise the ServerManager with the default Servers
	 */
	public void init() {
		//------------------------------------------------
		// Add Servers
		//------------------------------------------------
		addServer(new String[]{"IRC"}, new IRCServer(this));
	}
	
	/**
	 * Remove all Servers
	 */
	public void empty() {
		knownServers.clear();
	}
	
	/** Empty clone method to prevent cloning to get more copies of the ServerManager */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Add a Server type
	 *
	 * @param names String Array of valid names for this server
	 * @param server Server subclass for the Server.
	 */
	public void addServer(final String[] names, final Server server) {	
		Logger.debug("Adding Server: "+server.getName());
		
		try {
			for (int i = 0; i < names.length; ++i) {
				if (knownServers.containsKey(names[i].toLowerCase())) {
					knownServers.remove(names[i].toLowerCase());
				}
				Logger.debug2("\t Added server for: "+names[i]);
				knownServers.put(names[i].toLowerCase(), server);
			}
		} catch (Exception e) {
			Logger.error("Error adding Server '"+server.toString()+"': "+e.getMessage());
			delServer(server);
		}
	}
		
	/**
	 * Remove a Server type.
	 *
	 * @param server Server subclass for the Server.
	 */
	public void delServer(final Server server) {	
		Server testServer;
		String elementName;
		Logger.debug("Deleting Server: "+server.getName());
		for (Enumeration e = knownServers.keys(); e.hasMoreElements();) {
			elementName = (String)e.nextElement();
			Logger.debug2("\t Checking handler for: "+elementName);
			testServer = knownServers.get(elementName);
			if (testServer.getName().equalsIgnoreCase(server.getName())) {
				Logger.debug2("\t Removed handler for: "+elementName);
				knownServers.remove(elementName);
			}
		}
	}
	
	/**
	 * Get the Server type of a given name
	 *
	 * @param name Name to look for
	 * @return Server for the given name.
	 */
	public Server getServer(final String name) throws ServerNotFound {
		if (knownServers.containsKey(name.toLowerCase())) {
			return knownServers.get(name.toLowerCase());
		} else {
			throw new ServerNotFound("No Server is known by "+name);
		}
	}
	
	/**
	 * Get the valid server types
	 *
	 * @return Valid Servertypes as a collection
	 */
	public Collection<Server> getServerTypes() {
		return knownServers.values();
	}
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id: CommandManager.java 1360 2007-05-25 19:12:05Z ShaneMcC $"; }	
}


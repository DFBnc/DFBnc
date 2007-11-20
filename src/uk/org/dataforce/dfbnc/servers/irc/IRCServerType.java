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

import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.servers.ServerType;
import uk.org.dataforce.dfbnc.servers.ServerTypeManager;
import uk.org.dataforce.dfbnc.Account;

/**
 * This file gives the ability to connect to an IRC Server
 */
public class IRCServerType extends ServerType {
	/** CommandManager for IRCServerType */
	final CommandManager myCommandManager = new CommandManager();
	 
	/**
	 * Create a new instance of the ServerType Object
	 *
	 * @param manager ServerTypeManager that is in charge of this ServerType
	 */
	public IRCServerType (final ServerTypeManager manager) {
		super(manager);
		myCommandManager.addCommand(new ServerListCommand(myCommandManager));
		myCommandManager.addCommand(new IRCSetCommand(myCommandManager));
		myCommandManager.addCommand(new ConnectCommand(myCommandManager));
		myCommandManager.addCommand(new PerformCommand(myCommandManager));
	}
	
	/**
	 * Get the Description for this ServerType
	 *
	 * @return lower case name of this ServerType
	 */
	@Override
	public String getDescription() {
		return "This allows connecting to an IRC Server";
	}
	
	/**
	 * Called when this ServerType is activated
	 *
	 * @param account Account that activated the servertype
	 */
	@Override
	public void activate(final Account account) {
		account.getCommandManager().addSubCommandManager(myCommandManager);
		String nickname = account.getProperties().getProperty("irc.nickname", "");
		String altnickname = account.getProperties().getProperty("irc.altnickname", "");
		String username = account.getProperties().getProperty("irc.username", "");
		if (nickname.isEmpty()) {
			account.getProperties().setProperty("irc.nickname", account.getName());
		}
		if (altnickname.isEmpty()) {
			account.getProperties().setProperty("irc.altnickname", "_"+account.getName());
		}
		if (username.isEmpty()) {
			account.getProperties().setProperty("irc.username", account.getName());
		}
	}
	
	/**
	 * Called when this ServerType is deactivated
	 *
	 * @param account Account that deactivated the servertype
	 */
	@Override
	public void deactivate(final Account account) {
		account.getCommandManager().delSubCommandManager(myCommandManager);
	}
	
	/**
	 * Called to close any Active connections.
	 * This is called when an account is being disabled/removed or the BNC
	 * is shutting down.
	 *
	 * @param account Account to handle close for.
	 * @param reason Reason for closing.
	 */
	@Override
	public void close(final Account account, final String reason) {
	}
	
	/**
	 * Parse a String to get server information.
	 *
	 * @param input in the form Server[:port] [password]
	 * @return String[4] = {"server", "port", "password", "server:port password"}
	 *         Password will be set to "" if not specified
	 *         Port will be set to 6667 if not specified or an invalid port is specified
	 *         The last parameter will be set as the interpreted value of the input.
	 */
	public static String[] parseServerString(final String input) {
		String[] result = new String[]{"", "6667", "", ""};
		String[] parts = input.split(" ", 2);
		String[] hostBits = parts[0].split(":");
		// Set password
		if (parts.length > 1) { result[2] = parts[1].trim(); }
		// Set port
		if (hostBits.length > 1) {
			try {
				final int portNum;
				if (hostBits[1].charAt(0) == '+') {
					portNum = Integer.parseInt(hostBits[1].substring(1));
				} else {
					portNum = Integer.parseInt(hostBits[1]);
				}
				if (portNum > 0 && portNum <= 65535) {
					result[1] = hostBits[1];
				}
			} catch (NumberFormatException nfe) { /* Ignore the Wrong port and use default */ }
		}
		// Set Host
		result[0] = hostBits[0];
		// Set interpreted value
		result[3] = (result[0]+":"+result[1]+" "+result[2]).trim();
		
		return result;
	}
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }	
}
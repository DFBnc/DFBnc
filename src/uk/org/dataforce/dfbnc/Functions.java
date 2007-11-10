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

import uk.org.dataforce.util.MD5;

/**
 * This file stores various useful functions
 */
public final class Functions {
	/**
	 * Get the md5 hash of a string.
	 *
	 * @param string String to hash
	 * @return md5 hash of given string
	 */
	public static String md5(final String string) {
		return MD5.string(string);
	}
	
	/**
	 * Get the Server name that the BNC should use
	 *
	 * @param account If account has a ConnectionHandler it can define a different
	 *                name that we should use.
	 * @return Server name that the BNC Uses
	 */
	public static String getServerName(final Account account) {
		if (account != null) {
			if (account.getConnectionHandler() != null) {
				if (account.getConnectionHandler().getServerName() != null) {
					return account.getConnectionHandler().getServerName();
				}
			}
		}
		return Config.getOption("general", "ServerName", "DFBnc.Server");
	}
	
	/**
	 * Get the Bot name that the BNC Uses
	 *
	 * @return Bot name that the BNC Uses
	 */
	public static String getBotName() {
		return Config.getOption("general", "BotName", "-BNC");
	}
}
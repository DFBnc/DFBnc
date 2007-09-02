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
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;

/**
 * Functions related to Accountss
 */
public final class Account {
	//----------------------------------------------------------------------------
	// Static Variables
	//----------------------------------------------------------------------------
	/** List of loaded Accounts */
	private static Hashtable<String, Account> accounts = new Hashtable<String, Account>();
	/** Salt used when generating passwords */
	private static final String salt = "a5S5l1N4u4O2y9Z4l6W7t1A9b9L8a1X5a7F4s5E8";
	/** Are passwords case sensitive? */
	private static final boolean caseSensitivePasswords = false;

	//----------------------------------------------------------------------------
	// Per-Account Variables
	//----------------------------------------------------------------------------
	/** This account name */
	private final String myName;
	/** This account password */
	private String myPassword;
	/** Is this account an admin */
	private boolean isAdmin;
	/** How should -BNC contact the user? (PRIVMSG/NOTICE) */
	private String contactMethod;

	//----------------------------------------------------------------------------
	// Static Methods
	//----------------------------------------------------------------------------
	

	/**
	 * Get the number of known accounts.
	 *
	 * @return total number of known accounts
	 */
	protected static int count() {
		return accounts.size();
	}
	
	/**
	 * Check if a password matches an account.
	 *
	 * @param username Username to check
	 * @param password Password to check
	 * @return true/false depending on successful match
	 */
	protected static boolean checkPassword(final String username, final String password) {
		if (Account.exists(username)) {
			return Account.get(username).checkPassword(password);
		} else {
			return false;
		}
	}
	
	/**
	 * Check if an account exists
	 *
	 * @param username Username to check
	 * @return true/false depending on if the account exists or not
	 */
	protected static boolean exists(final String username) {
		return accounts.containsKey(username.replace('.', '_').toLowerCase());
	}
	
	/**
	 * Get an account object
	 *
	 * @param username Username to check
	 * @return Account object for given username, or null if it doesn't exist
	 */
	protected static Account get(final String username) {
		return accounts.get(username.replace('.', '_').toLowerCase());
	}
	
	/**
	 * Create an account with the given username and password and return the
	 * Account Object associated with it.
	 *
	 * @param username Username to check
	 * @param password Password to check
	 * @return true/false depending on successful match
	 */
	protected static Account createAccount(final String username, final String password) {
		// Update total count.
		Account acc = new Account(username);
		acc.setPassword(password);
		Config.setIntOption("users", "count", accounts.size());
		return acc;
	}
	
	/**
	 * Load all the accounts from the config
	 */
	protected static void loadAccounts() {
		final Enumeration values = Config.getProperties().propertyNames();
		while (values.hasMoreElements()) {
			final String name = ((String)values.nextElement()).toLowerCase();
			
			if (name.startsWith("user_") && name.endsWith(".password")) {
				// Turn "user_foo.password" into "foo"
				String accname = name.split("_", 2)[1].split("\\.", 2)[0];
				
				Logger.debug("Loading account: "+accname);
				new Account(accname);
			}
		}
	}
	
	/**
	 * Save all the accounts to the config
	 */
	protected static void saveAccounts() {
		for (Account acc : accounts.values()) {
			Logger.debug("Saving account: "+acc.getName());
			acc.save();
		}
	}
	
	//----------------------------------------------------------------------------
	// Per-Account Methods
	//----------------------------------------------------------------------------

	/**
	 * Create an Account object.
	 * This will load all the settings for the account from the config file.
	 *
	 * @param username Name of this account
	 */
	private Account(final String username) {
		final String configName = "user_"+username.replace('.', '_');
		myName = username;
		
		// Get settings
		myPassword = Config.getOption(configName, "password", "...");
		isAdmin = Config.getBoolOption(configName, "admin", false);
		contactMethod = Config.getOption(configName, "contactMethod", "PRIVMSG");
		
		// Add to hashtable
		accounts.put(username.toLowerCase(), this);
	}
	
	/**
	 * Get the name of this account
	 *
	 * @return Name of this account
	 */
	public String getName() { return myName; }
	
	/**
	 * Save the account settings for this account to the config file
	 */
	protected void save() {
		final String configName = "user_"+myName.replace('.', '_');
		
		Config.setOption(configName, "password", myPassword);
		Config.setBoolOption(configName, "admin", isAdmin);
	}
	
	/**
	 * Check if a password matches this account password.
	 *
	 * @param password Password to check
	 * @return true/false depending on successful match
	 */
	public boolean checkPassword(final String password) {
		StringBuffer hashedPassword = new StringBuffer(myName.toLowerCase());
		if (caseSensitivePasswords) { hashedPassword.append(password); }
		else {hashedPassword.append(password.toLowerCase());}
		hashedPassword.append(salt);
		
		return Functions.md5(hashedPassword.toString()).equals(myPassword);
	}
	
	/**
	 * Change the password of this account
	 *
	 * @param password New password
	 */
	public void setPassword(final String password) {
		StringBuffer hashedPassword = new StringBuffer(myName.toLowerCase());
		if (caseSensitivePasswords) { hashedPassword.append(password); }
		else {hashedPassword.append(password.toLowerCase());}
		hashedPassword.append(salt);
		
		myPassword = Functions.md5(hashedPassword.toString());
	}
	
	/**
	 * Change the admin setting for this account
	 *
	 * @param value true/false for new value of isAdmin
	 */
	public void setAdmin(final boolean value) {
		isAdmin = value;
	}

	/**
	 * Return the value of isAdmin.
	 *
	 * @return the value of isAdmin
	 */
	public boolean isAdmin() {
		return isAdmin;
	}
	
	/**
	 * Change the contactMethod setting for this account
	 *
	 * @param value new value for contactMethod
	 */
	public void setContactMethod(final String value) {
		contactMethod = value;
	}

	/**
	 * Get the contactMethod setting for this account
	 *
	 * @return value for contactMethod
	 */
	public String getContactMethod() {
		return contactMethod;
	}
}
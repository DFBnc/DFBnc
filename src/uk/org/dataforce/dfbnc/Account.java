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

import uk.org.dataforce.libs.util.TypedProperties;
import uk.org.dataforce.libs.logger.Logger;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.servers.ServerType;
import uk.org.dataforce.dfbnc.servers.ServerTypeNotFound;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;

/**
 * Functions related to Accounts
 */
public final class Account implements UserSocketWatcher {
    //----------------------------------------------------------------------------
    // Static Variables
    //----------------------------------------------------------------------------
    /** List of loaded Accounts */
    private static final HashMap<String, Account> accounts = new HashMap<String, Account>();
    /** Salt used when generating passwords */
    private static final String salt = "a5S5l1N4u4O2y9Z4l6W7t1A9b9L8a1X5a7F4s5E8";
    /** Are passwords case sensitive? */
    private static final boolean caseSensitivePasswords = false;

    //----------------------------------------------------------------------------
    // Per-Account Variables
    //----------------------------------------------------------------------------
    /** This account name */
    private final String myName;
    /** Is this account an admin */
    private boolean isAdmin;
    /** Propeties file with all the relevent settings for this account */
    private TypedProperties accountOptions = new TypedProperties();
    /** Deletecode for this account. This is not saved between sessions */
    private String deleteCode = "";
    /** CommandManager for this account */
    private CommandManager myCommandManager = new CommandManager();
    /** ConnectionHandler for this account */
    private ConnectionHandler myConnectionHandler = null;
    /** List of all sockets that are part of this account. */
    private List<UserSocket> myUserSockets = new ArrayList<UserSocket>();

    //----------------------------------------------------------------------------
    // Static Methods
    //----------------------------------------------------------------------------
    

    /**
     * Get the number of known accounts.
     *
     * @return total number of known accounts
     */
    public static int count() {
        return accounts.size();
    }
    
    /**
     * Check if a password matches an account.
     *
     * @param username Username to check
     * @param password Password to check
     * @return true/false depending on successful match
     */
    public static boolean checkPassword(final String username, final String password) {
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
    public static boolean exists(final String username) {
        return accounts.containsKey(username.replace('.', '_').toLowerCase());
    }
    
    /**
     * Get an account object
     *
     * @param username Username to check
     * @return Account object for given username, or null if it doesn't exist
     */
    public static Account get(final String username) {
        return accounts.get(username.replace('.', '_').toLowerCase());
    }
    
    /**
     * Create an account with the given username and password and return the
     * Account Object associated with it.
     *
     * @param username Username to check
     * @param password Password to check
     * @return The account created, or null if the account could not be created
     */
    public static Account createAccount(final String username, final String password) {
        synchronized (accounts) {
            if (!exists(username)) {
                Account acc = new Account(username);
                acc.setPassword(password);
                Config.setIntOption("users", "count", accounts.size());
                return acc;
            } else {
                return null;
            }
        }
    }
    
    /**
     * Create a random password 8 characters in length.
     *
     * @return Random Password
     */
    public static String makePassword() {
        return makePassword(8);
    }
    
    /**
     * Create a random password X characters in length.
     *
     * @param length Length to make password
     * @return Random Password
     */
    public static String makePassword(final int length) {
        final String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!£$%^&*()_-+={}[]@~'#<>?/.,\\|\"";
        final StringBuffer password = new StringBuffer();
        final Random r = new Random();
        for (int i = 0; i < length; i++) {
            password.append(validChars.charAt(r.nextInt(validChars.length())));
        }
        return password.toString();
    }
    
    /**
     * Load all the accounts from the config
     */
    public static void loadAccounts() {
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
    public static void saveAccounts() {
        for (Account acc : accounts.values()) {
            Logger.debug("Saving account: "+acc.getName());
            acc.save();
        }
    }
    
    /**
     * Shutdown all accounts.
     */
    public static void shutdown() {
        for (Account acc : accounts.values()) {
            ServerType st = acc.getServerType();
            if (st != null) {
                st.close(acc, "BNC Shutting Down");
            }
            if (acc.getConnectionHandler() != null) {
                acc.getConnectionHandler().shutdown("BNC Shutting Down");
            }
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
        final String configName = "user_"+username.replace('.', '_')+".";
        myName = username;
        
        // Set Default settings
        accountOptions.setCaseSensitivity(false);
        accountOptions.setProperty("password", "...");
        accountOptions.setBoolProperty("admin", false);
        accountOptions.setProperty("contactMethod", "SNOTICE");
        accountOptions.setBoolProperty("first", true);
        
        // Get actual settings
        for (Object obj : Config.getProperties().keySet()) {
            String name = (String)obj;
            if (name.toLowerCase().startsWith(configName)) {
                // Turn "user_foo.password" into "password"
                String option = name.split("\\.", 2)[1];
                accountOptions.setProperty(option, Config.getProperties().getProperty(name));
            }
        }
        
        // Enable global commands.
        myCommandManager.addSubCommandManager(DFBnc.getUserCommandManager());
        if (isAdmin) {
            myCommandManager.addSubCommandManager(DFBnc.getAdminCommandManager());
        }
        
        final ServerType myServerType = getServerType();
        if (myServerType != null) {
            myServerType.activate(this);
        }
        
        // Add to HashMap
        accounts.put(username.toLowerCase(), this);
    }
    
    /**
     * Called when a new UserSocket is opened on an account that this class is
     * linked to.
     *
     * @param user UserSocket for user
     */
    @Override
    public void userConnected(final UserSocket user) {
        myUserSockets.add(user);
        if (myConnectionHandler != null && myConnectionHandler instanceof UserSocketWatcher) {
            ((UserSocketWatcher)myConnectionHandler).userConnected(user);
        }
        for (UserSocket socket : myUserSockets) {
            if (user != socket) {
                socket.sendBotMessage("Another client has connected ("+user.getIP()+")");
            }
        }
    }
    
    /**
     * Called when a UserSocket is closed on an account that this class is
     * linked to.
     *
     * @param user UserSocket for user
     */
    @Override
    public void userDisconnected(final UserSocket user) {
        myUserSockets.remove(user);
        if (myConnectionHandler != null && myConnectionHandler instanceof UserSocketWatcher) {
            ((UserSocketWatcher)myConnectionHandler).userDisconnected(user);
        }
        for (UserSocket socket : myUserSockets) {
            if (user != socket) {
                socket.sendBotMessage("Client has Disconnected ("+user.getIP()+")");
            }
        }
    }
    
    /**
     * Get a List of all UserSockets that are part of this account
     *
     * @return a List of all UserSockets that are part of this account
     */
    public List<UserSocket> getUserSockets() {
        return myUserSockets;
    }
    
    /**
     * Get the ServerType for this account
     *
     * @return The ServerType for this account (or null if not defined, or invalid)
     */
    public ServerType getServerType() {
        final String currentType = getProperties().getProperty("servertype", "");
        if (!currentType.isEmpty()) {
            try {
                return DFBnc.getServerTypeManager().getServerType(currentType);
            } catch (ServerTypeNotFound stnf) {
                return null;
            }
        } else {
            return null;
        }
    }
    
    /**
     * Get the CommandManager for this account
     *
     * @return The CommandManager for this account
     */
    public CommandManager getCommandManager() {
        return myCommandManager;
    }
    
    /**
     * Get the ConnectionHandler for this account
     *
     * @return The ConnectionHandler for this account
     */
    public ConnectionHandler getConnectionHandler() {
        return myConnectionHandler;
    }
    
    /**
     * Set the ConnectionHandler for this account
     *
     * @param handler The New ConnectionHandler for this account
     */
    public void setConnectionHandler(final ConnectionHandler handler) {
        myConnectionHandler = handler;
    }
    
    /**
     * Save the account settings for this account to the config file
     */
    public void save() {
        final String configName = "user_"+myName.replace('.', '_');
        
        // Store settings in main config
        for (Object obj : accountOptions.keySet()) {
            String name = (String)obj;
            Logger.debug2("Saving property: "+configName+"."+name+" -> "+accountOptions.getProperty(name));
            Config.setOption(configName, name, accountOptions.getProperty(name));
        }
    }
    
    /**
     * Get the name of this account
     *
     * @return Name of this account
     */
    public String getName() { return myName; }
    
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
        
        return Functions.md5(hashedPassword.toString()).equals(accountOptions.getProperty("password"));
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
        
        accountOptions.setProperty("password", Functions.md5(hashedPassword.toString()));
    }
    
    /**
     * Get the DeleteCode for this account
     *
     * @return The DeleteCode for this account
     */
    public String getDeleteCode() {
        return deleteCode;
    }
    
    /**
     * Set the Delete Code for this account
     *
     * @param deleteCode The New DeleteCode for this account
     */
    public void setDeleteCode(final String deleteCode) {
        this.deleteCode = deleteCode;
    }
    
    /**
     * Delete this account
     */
    public void delete() {
        accounts.remove(myName);
        Config.setIntOption("users", "count", accounts.size());
        for (UserSocket socket : myUserSockets) {
            socket.sendLine(":%s NOTICE :Connection terminating (Account Deleted)", Functions.getServerName(socket.getAccount()));
            socket.close();
        }
        myConnectionHandler.shutdown("Account Deleted");
    }
    
    /**
     * Change the suspended setting for this account
     *
     * @param value true/false for new value of isSuspended
     * @param reason Reason for account suspension
     */
    public void setSuspended(final boolean value, final String reason) {
        accountOptions.setBoolProperty("suspended", value);
        if (value) {
            final String suspendReason = (reason != null && !reason.isEmpty()) ? reason : "No reason specified";
            accountOptions.setProperty("suspendReason", suspendReason);
            
            for (UserSocket socket : myUserSockets) {
                socket.sendLine(":%s NOTICE :Connection terminating - Account Suspended (%s)", Functions.getServerName(socket.getAccount()), suspendReason);
                socket.close();
            }
            myConnectionHandler.shutdown("Account Suspended");
        }
    }

    /**
     * Is the account suspended?
     *
     * @return Is the account suspended?
     */
    public boolean isSuspended() {
        return accountOptions.getBoolProperty("suspended", false);
    }
    
    /**
     * Why is the account suspended?
     *
     * @return Reason why the account is suspended
     */
    public String getSuspendReason() {
        if (isSuspended()) {
            return accountOptions.getProperty("suspendReason");
        } else {
            return "";
        }
    }
    
    /**
     * Change the admin setting for this account
     *
     * @param value true/false for new value of isAdmin
     */
    public void setAdmin(final boolean value) {
        if (value != isAdmin) {
            // Change command manager to reflect new setting
            if (value) {
                myCommandManager.addSubCommandManager(DFBnc.getAdminCommandManager());
            } else {
                myCommandManager.delSubCommandManager(DFBnc.getAdminCommandManager());
            }
        }
        accountOptions.setBoolProperty("admin", value);
    }

    /**
     * Is the account an admin
     *
     * @return Is the account an admin?
     */
    public boolean isAdmin() {
        return accountOptions.getBoolProperty("admin", false);
    }
    
    /**
     * Change the first-time setting for this account
     *
     * @param value true/false for new value of isFirst
     */
    public void setFirst(final boolean value) {
        accountOptions.setBoolProperty("first", value);
    }
    
    /**
     * Return the value of isFirst.
     *
     * @return the value of isFirst
     */
    public boolean isFirst() {
        return accountOptions.getBoolProperty("first", true);
    }
    
    /**
     * Change the contactMethod setting for this account
     *
     * @param value new value for contactMethod
     */
    public void setContactMethod(final String value) {
        accountOptions.setProperty("contactMethod", value);
    }

    /**
     * Get the contactMethod setting for this account
     *
     * @return value for contactMethod
     */
    public String getContactMethod() {
        return accountOptions.getProperty("contactMethod", "SNOTICE");
    }
    
    /**
     * Get the accountOptions TypedProperties file for this account.
     *
     * @return accountOptions
     */
    public TypedProperties getProperties() {
        return accountOptions;
    }
}

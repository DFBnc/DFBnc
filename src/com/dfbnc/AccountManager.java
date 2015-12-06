/*
 *
 * Copyright (c) 2006-2013 Shane Mc Cormack, Gregory Holmes
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
 */

package com.dfbnc;

import com.dfbnc.servers.ServerType;
import com.dfbnc.sockets.UnableToConnectException;
import com.dmdirc.util.io.InvalidConfigFileException;
import uk.org.dataforce.libs.logger.LogLevel;
import uk.org.dataforce.libs.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages the list of accounts.
 */
public class AccountManager {

    /** List of loaded Accounts */
    private final Map<String, Account> accounts = new HashMap<>();

    /** Prevent public instantiation of AccountManager. Use {@link DFBnc#getAccountManager()}. */
    AccountManager() {
    }

    /**
     * Gets a collection of known accounts.
     *
     * @return Returns a collection of accounts
     */
    public Collection<Account> getAccounts() {
        return new ArrayList<>(accounts.values());
    }

    /**
     * Get the number of known accounts.
     *
     * @return total number of known accounts
     */
    public int count() {
        return accounts.size();
    }

    /**
     * Check if a password matches an account.
     *
     * @param username Username to check
     * @param subclient Subclient to check, "" for none.
     * @param password Password to check
     * @return true/false depending on successful match
     */
    public boolean checkPassword(final String username, final String subclient, final String password) {
        if (exists(username)) {
            return get(username).checkPassword(subclient, password);
        } else {
            Logger.debug2("CheckPassword: User does not exist "+username);
            return false;
        }
    }

    /**
     * Check if an account exists
     *
     * @param username Username to check
     * @return true/false depending on if the account exists or not
     */
    public boolean exists(final String username) {
        Logger.debug2("exists: Checking if user exists: " + username.replace('.', '_').toLowerCase());
        if (LogLevel.DEBUG3.isLoggable(Logger.getLevel())) {
            for (String a : accounts.keySet()) {
                Logger.debug3("exists: Found acc: " + a);
            }
        }
        return accounts.containsKey(username.replace('.', '_').toLowerCase());
    }

    /**
     * Get an account object
     *
     * @param username Username to check
     * @return Account object for given username, or null if it doesn't exist
     */
    public Account get(final String username) {
        return accounts.get(username.replace('.', '_').toLowerCase());
    }

    /**
     * Remove an account object
     *
     * @param username Username to remove
     * @return Account object that was removed, or null if nothing was removed.
     */
    public Account remove(final String username) {
        synchronized (accounts) {
            return accounts.remove(username.replace('.', '_').toLowerCase());
        }
    }

    /**
     * Create an account with the given username and password and return the
     * Account Object associated with it.
     *
     * @param username Username to create
     * @param password Password for the user
     *
     * @return The account created, or null if the account could not be created
     */
    public Account createAccount(final String username, final String password) {
        final String accountName = username.replace('.', '_').toLowerCase();
        Logger.debug2("createAccount: Saving user as: " + username.replace('.', '_').toLowerCase());
        synchronized (accounts) {
            Account acc = null;
            if (!exists(accountName)) {
                Logger.debug2("Creating new account: "+accountName);
                try {
                    acc = new Account(accountName);
                } catch (IOException | InvalidConfigFileException ex) {
                    Logger.error("Error creating account: " + ex.getMessage());
                }
                if (acc != null) {
                    Logger.debug2("Account created.");
                    acc.setPassword(password);
                    accounts.put(accountName, acc);
                }
            }
            return acc;
        }
    }

    /**
     * Create a random password 8 characters in length.
     *
     * @return Random Password
     */
    public String makePassword() {
        return makePassword(8);
    }

    /**
     * Create a random password X characters in length.
     *
     * @param length Length to make password
     * @return Random Password
     */
    public String makePassword(final int length) {
        final String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!£$%^&*()_-+={}[]@~'#<>?/.,\\|\"";
        return makePassword(length, validChars);
    }

    /**
     * Create a random password X characters in length from the given characters.
     *
     * @param length Length to make password
     * @param validChars Valid characters to pick from.
     * @return Random Password
     */
    public String makePassword(final int length, final String validChars) {
        final StringBuilder password = new StringBuilder();
        final Random r = new SecureRandom();
        for (int i = 0; i < length; i++) {
            password.append(validChars.charAt(r.nextInt(validChars.length())));
        }
        return password.toString();
    }

    /**
     * Load all the accounts from the config
     */
    public void loadAccounts() {
        final File directory = new File(DFBnc.getConfigDirName());
        final File[] directories = directory.listFiles();
        if (directories == null) {
            return;
        }
        for (File file : directories) {
            if (!DFBnc.getConfigFileName().equals(file.getName())) {
                try {
                   Account acc = new Account(file.getName());
                   accounts.put(acc.getName(), acc);

                    if (acc.getAccountConfig().getOptionBool("server", "autoconnect")) {
                       final ServerType type = acc.getServerType();
                       if (type != null) {
                           try {
                              final ConnectionHandler handler = type.newConnectionHandler(acc, -1);
                              acc.setConnectionHandler(handler);
                           }  catch (UnableToConnectException ex) {
                               Logger.error("Unable to autoconnect account: " + file.getName());
                           }
                       }
                   }
               } catch (IOException | InvalidConfigFileException ex) {
                   Logger.error("Unable to load account: " + file.getName() + "(" + ex.getMessage() + ")");
               }
            }
        }
    }

    /**
     * Save all the accounts to the config
     */
    public void saveAccounts() {
        for (Account acc : accounts.values()) {
            Logger.debug("Saving account: " + acc.getName());
            acc.save();
        }
    }

    /**
     * Shutdown all accounts.
     */
    public void shutdown() {
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
}

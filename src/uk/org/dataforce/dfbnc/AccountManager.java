/*
 * 
 * Copyright (c) 2006-2008 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

package uk.org.dataforce.dfbnc;

import uk.org.dataforce.dfbnc.config.InvalidConfigFileException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import uk.org.dataforce.dfbnc.servers.ServerType;
import uk.org.dataforce.libs.logger.Logger;

/**
 *
 */
public class AccountManager {

    /** List of loaded Accounts */
    private static final HashMap<String, Account> accounts =
            new HashMap<String, Account>();

    /** Prevent instantiation of AccountManager. */
    private AccountManager() {
    }

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
    public static boolean checkPassword(final String username,
            final String password) {
        if (exists(username)) {
            return get(username).checkPassword(password);
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
     *
     * @return The account created, or null if the account could not be created
     */
    public static Account createAccount(final String username,
            final String password) {
        final String accountName = username.replace('.', '_').toLowerCase();
        synchronized (accounts) {
            Account acc = null;
            if (!exists(accountName)) {
                try {
                    acc = new Account(accountName);
                } catch (IOException ex) {
                    Logger.error("Error creating account: " + ex.getMessage());
                } catch (InvalidConfigFileException ex) {
                    Logger.error("Error creating account: " + ex.getMessage());
                }
                if (acc != null) {
                    acc.setPassword(password);
                    accounts.put(username, acc);
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
        final String validChars =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!£$%^&*()_-+={}[]@~'#<>?/.,\\|\"";
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
        final File directory = new File(DFBnc.getConfigDirName());
        for (File file : directory.listFiles()) {
            try {
                Account acc = new Account(file.getName());
                accounts.put(acc.getName(), acc);
            } catch (IOException ex) {
                Logger.error("Unable to load account: " + file.getName() + "(" +
                        ex.getMessage() + ")");
            } catch (InvalidConfigFileException ex) {
                Logger.error("Unable to load account: " + file.getName() + "(" +
                        ex.getMessage() + ")");
            }
        }
    }

    /**
     * Save all the accounts to the config
     */
    public static void saveAccounts() {
        for (Account acc : accounts.values()) {
            Logger.debug("Saving account: " + acc.getName());
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
}

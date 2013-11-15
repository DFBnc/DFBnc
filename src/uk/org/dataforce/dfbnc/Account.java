/*
 * Copyright (c) 2006-2013 Shane Mc Cormack
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

import com.dmdirc.util.io.ConfigFile;
import com.dmdirc.util.io.InvalidConfigFileException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.config.Config;
import uk.org.dataforce.dfbnc.config.DefaultsConfig;
import uk.org.dataforce.dfbnc.servers.ServerType;
import uk.org.dataforce.dfbnc.servers.ServerTypeNotFound;
import uk.org.dataforce.dfbnc.sockets.UnableToConnectException;
import uk.org.dataforce.dfbnc.sockets.UserSocket;
import uk.org.dataforce.dfbnc.sockets.UserSocketWatcher;
import uk.org.dataforce.libs.logger.Logger;
import uk.org.dataforce.libs.util.Util;

/**
 * Functions related to Accounts
 */
public final class Account implements UserSocketWatcher {

    /** Salt used when generating passwords */
    private static final String salt = "a5S5l1N4u4O2y9Z4l6W7t1A9b9L8a1X5a7F4s5E8";
    /** Are passwords case sensitive? */
    private static final boolean caseSensitivePasswords = false;
    /** This account name */
    private final String myName;
    /** Is this account an admin */
    private boolean isAdmin;
    /** Deletecode for this account. This is not saved between sessions */
    private String deleteCode = "";
    /** CommandManager for this account */
    private CommandManager myCommandManager = new CommandManager();
    /** ConnectionHandler for this account */
    private ConnectionHandler myConnectionHandler = null;
    /** List of all sockets that are part of this account. */
    private List<UserSocket> myUserSockets = new CopyOnWriteArrayList<UserSocket>();
    /** Account config file. */
    private Config config;
    /** Reconnect Timer. */
    private Timer reconnectTimer;
    /** Is the next disconnect intentional? */
    private boolean disconnectWanted;

    /**
     * Create an Account object.
     * This will load all the settings for the account from the config file.
     *
     * @param username Name of this account
     *
     * @throws IOException Error loading config
     * @throws InvalidConfigFileException  Error loading config
     */
    public Account(final String username) throws IOException, InvalidConfigFileException {
        myName = username;

        final File confDir = new File(DFBnc.getConfigDirName(), username);
        if (!confDir.exists()) {
            if (!confDir.mkdirs()) {
                throw new IOException("Unable to create config directory.");
            }
        }
        config = new DefaultsConfig(
                new ConfigFile(new File(confDir, username + ".conf")),
                new ConfigFile(DFBnc.class.getResourceAsStream("/uk/org/dataforce/dfbnc/defaults.config")));

        // Enable global commands.
        myCommandManager.addSubCommandManager(DFBnc.getUserCommandManager());
        if (isAdmin()) {
            myCommandManager.addSubCommandManager(DFBnc.getAdminCommandManager());
        }

        final ServerType myServerType = getServerType();
        if (myServerType != null) {
            myServerType.activate(this);
        }
    }

    /**
     * Get the Server name that the BNC should use when dealing with this account.
     *
     * @return Server name that the BNC Uses
     */
    public String getServerName() {
        if (getConnectionHandler() != null && getConnectionHandler().getServerName() != null) {
            return getConnectionHandler().getServerName();
        }
        return DFBnc.getBNC().getDefaultServerName();
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
            Logger.debug2("Handle userConnected: "+myConnectionHandler+" -> "+user);
            ((UserSocketWatcher) myConnectionHandler).userConnected(user);
            Logger.debug2("Handled userConnected");
        } else {
            user.setSyncCompleted();
        }

        new Timer().schedule(new TimerTask() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                synchronized (user) {
                    if (user.getSocketAnnouncement()) { return; }
                    user.setSocketAnnouncement(true);
                    final StringBuilder sb = new StringBuilder("Another client has connected (");
                    sb.append(user.getIP());

                    if (user.getClientID() != null) {
                        sb.append(" [");
                        sb.append(user.getClientID());
                        sb.append("]");
                    }
                    if (user.getClientVersion() != null) {
                        sb.append(" - \"");
                        sb.append(user.getClientVersion());
                        sb.append("\"");
                    }

                    sb.append(")");

                    for (UserSocket socket : myUserSockets) {
                        if (user != socket) {
                            socket.sendBotMessage(sb.toString());
                        }
                    }
                }
            }
        }, 1000);
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
            ((UserSocketWatcher) myConnectionHandler).userDisconnected(user);
        }

        if (!user.getSocketAnnouncement()) {
            return;
        }
        user.setSocketAnnouncement(true);
        final StringBuilder sb = new StringBuilder("Client has Disconnected (");
        sb.append(user.getIP());

        if (user.getClientID() != null) {
            sb.append(" [");
            sb.append(user.getClientID());
            sb.append("]");
        }
        if (user.getClientVersion() != null) {
            sb.append(" - \"");
            sb.append(user.getClientVersion());
            sb.append("\"");
        }

        sb.append("): ");
        sb.append(user.getCloseReason());

        for (UserSocket socket : myUserSockets) {
            if (user != socket) {
                socket.sendBotMessage(sb.toString());
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
     * Send a message to all connected users from the bnc bot in printf format.
     *
     * @param data The format string
     * @param args The args for the format string
     */
    public void sendBotMessage(final String data, final Object... args) {
        for (UserSocket user : myUserSockets) {
            user.sendBotMessage(data, args);
        }
    }

    /**
     * Get the ServerType for this account
     *
     * @return The ServerType for this account (or null if not defined, or invalid)
     */
    public ServerType getServerType() {
        final String currentType = config.getOption("server", "servertype");
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
        config.save();
    }

    /**
     * Get the name of this account
     *
     * @return Name of this account
     */
    public String getName() {
        return myName;
    }

    /**
     * Check if a password matches this account password.
     *
     * @param password Password to check
     * @return true/false depending on successful match
     */
    public boolean checkPassword(final String password) {
        return checkPassword(null, password);
    }

    /**
     * Check if a password matches this account password. If no subclient
     * password is defined, fallback to the main account password.
     *
     * @param subclient Subclient to check, null for none.
     * @param password Password to check
     * @return true/false depending on successful match
     */
    public boolean checkPassword(final String subclient, final String password) {
        final String passwordKey = "password" + ((subclient != null && subclient.length() > 0) ? "." + subclient.toLowerCase() : "");
        final StringBuilder hashedPassword = new StringBuilder(myName.toLowerCase());
        final boolean hasSubClientPassword = subclient != null && config.hasOption("user", passwordKey);
        if (hasSubClientPassword) {
            hashedPassword.append(subclient.toLowerCase());
        }
        if (caseSensitivePasswords) {
            hashedPassword.append(password);
        } else {
            hashedPassword.append(password.toLowerCase());
        }
        hashedPassword.append(salt);

        final String check = hasSubClientPassword ? config.getOption("user", passwordKey) : config.getOption("user", "password");

        return Util.md5(hashedPassword.toString()).equals(check);
    }

    /**
     * Change the password of this account
     *
     * @param password New password
     */
    public void setPassword(final String password) {
        setPassword(null, password);
    }

    /**
     * Change the password of this account
     *
     * @param subclient Subclient to set password for, null for none.
     * @param password New password
     */
    public void setPassword(final String subclient, final String password) {
        StringBuilder hashedPassword = new StringBuilder(myName.toLowerCase());
        if (subclient != null) {
            hashedPassword.append(subclient.toLowerCase());
        }
        if (caseSensitivePasswords) {
            hashedPassword.append(password);
        } else {
            hashedPassword.append(password.toLowerCase());
        }
        hashedPassword.append(salt);

        final String passwordKey = "password" + ((subclient != null && subclient.length() > 0) ? "." + subclient.toLowerCase() : "");

        config.setOption("user", passwordKey, Util.md5(hashedPassword.toString()));
        config.save();
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
    public void delete() throws IOException {
        // Suspend so that reconnecting doesn't work.
        config.setOption("user", "suspended", true);
        config.setOption("user", "suspendReason", "Account deleted.");

        // Disconnect all users and the connection handler
        for (UserSocket socket : myUserSockets) {
            socket.close("Account deleted.");
        }
        if (myConnectionHandler != null) {
            myConnectionHandler.shutdown("Account Deleted");
        }

        final File confDir = new File(DFBnc.getConfigDirName(), getName());
        if (confDir.exists()) {
            if (!Util.deleteFolder(confDir)) {
                throw new IOException("Unable to delete config directory.");
            }
        }

        // Now remove the account.
        AccountManager.remove(getName());
    }

    /**
     * Change the suspended setting for this account
     *
     * @param value true/false for new value of isSuspended
     * @param reason Reason for account suspension
     */
    public void setSuspended(final boolean value, final String reason) {
        config.setOption("user", "suspended", value);
        if (value) {
            final String suspendReason = (reason != null && !reason.isEmpty()) ? reason : "No reason specified";
            config.setOption("user", "suspendReason", suspendReason);

            for (UserSocket socket : myUserSockets) {
                socket.close("Account Suspended (" + suspendReason + ")");
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
        return config.getOptionBool("user", "suspended");
    }

    /**
     * Why is the account suspended?
     *
     * @return Reason why the account is suspended
     */
    public String getSuspendReason() {
        if (isSuspended()) {
            return config.getOption("user", "suspendReason");
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
        config.setOption("user", "admin", value);
    }

    /**
     * Is the account an admin
     *
     * @return Is the account an admin?
     */
    public boolean isAdmin() {
        return config.getOptionBool("user", "admin");
    }

    /**
     * Change the first-time setting for this account
     *
     * @param value true/false for new value of isFirst
     */
    public void setFirst(final boolean value) {
        config.setOption("user", "first", value);
    }

    /**
     * Return the value of isFirst.
     *
     * @return the value of isFirst
     */
    public boolean isFirst() {
        return config.getOptionBool("user", "first");
    }

    /**
     * Change the contactMethod setting for this account
     *
     * @param value new value for contactMethod
     */
    public void setContactMethod(final String value) {
        config.setOption("user", "contactMethod", value);
    }

    /**
     * Get the contactMethod setting for this account
     *
     * @return value for contactMethod
     */
    public String getContactMethod() {
        return config.getOption("user", "contactMethod");
    }

    /**
     * Returns this accounts config.
     *
     * @return Account's config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Are we currently trying to reconnect?
     *
     * @return True if there is a reconnectTimer around
     */
    public boolean isReconnecting() {
        return reconnectTimer != null;
    }

    /**
     * Cancel any ongoing reconnection attempts.
     */
    public void cancelReconnect() {
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
    }

    /**
     * Calling this will prevent the next disconnect causing a reconnect.
     */
    public void disableReconnect() {
        disconnectWanted = true;
    }

    /**
     * Called when the IRC Connnection handler is disconnected.
     *
     * @param reason Reason for disconnection
     */
    public void handlerDisconnected(final String reason) {
        final ConnectionHandler oldHandler = myConnectionHandler;
        myConnectionHandler = null;
        if (!disconnectWanted && config.getOptionBool("server", "reconnect")) {
            reconnectTimer = new Timer("Reconnect Timer - " + getName());

            reconnectTimer.schedule(new TimerTask(){
                @Override
                public void run() {
                    try {
                        sendBotMessage("Attempting reconnect...");
                        if (oldHandler == null) {
                            sendBotMessage("Reconnect failed. No handler to reconnect.");
                            myConnectionHandler = null;
                        } else {
                            final ConnectionHandler newHandler = oldHandler.newInstance();
                            setConnectionHandler(newHandler);
                        }
                    } catch (final UnableToConnectException ex) {
                        sendBotMessage("Unable to reconnect: " + ex.getMessage());
                        myConnectionHandler = null;
                        // This is not the place to try again, this exception
                        // happens if the params we have are not valid for
                        // creating a connection.
                        // Errors actually trying to make the connection will
                        // result in a handlerDisconnected() call.
                    } catch (final Throwable t) {
                        reportException(t, "Unhandled Exception");
                    }
                    cancelReconnect();
                }
            }, 5000);
        }

        if (config.getOptionBool("server", "userdisconnect")) {
            for (UserSocket socket : getUserSockets()) {
                // Only disconnect users if they have had a 001.
                if (socket.getPost001()) {
                    socket.sendLine("ERROR : " + reason, false);
                    socket.closeSocket("Error from server: " + reason);
                } else {
                    socket.sendBotMessage("Disconnected from server: " + reason);
                }
            }
        } else {
            for (UserSocket socket : getUserSockets()) {
                oldHandler.cleanupUser(socket, reason);
            }
        }

        disconnectWanted = false;
    }

    /**
     * Report an exception to the console and any connected user.
     *
     * @param t Throwable to report.
     */
    public void reportException(final Throwable t) {
        reportException(t, "Exception");
    }

    /**
     * Report an exception to the console and any connected user.
     *
     * @param t Throwable to report.
     * @param type Human-Friendly type of exception.
     */
    public void reportException(final Throwable t,  final String type) {
        if (!getConfig().getOptionBool("server", "reporterrors") || t == null) {
            return;
        }

        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        Logger.error("----");
        Logger.error(type + ": " + t);
        Logger.error(type + " Stack trace: " + writer.getBuffer());
        Logger.error("----");

        sendBotMessage("----");
        sendBotMessage(type + ": " + t);
        sendBotMessage(type + " Stack trace:");
        final String[] bits = writer.getBuffer().toString().split("\n");
        for (final String bit : bits) {
            sendBotMessage("    " + bit);
        }
        sendBotMessage("----");
    }
}

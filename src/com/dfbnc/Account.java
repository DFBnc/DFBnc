/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

import com.dfbnc.commands.CommandManager;
import com.dfbnc.config.Config;
import com.dfbnc.config.ConfigChangeListener;
import com.dfbnc.config.ConfigFileConfig;
import com.dfbnc.config.DefaultsConfig;
import com.dfbnc.servers.logging.ServerLogger;
import com.dfbnc.servers.ServerType;
import com.dfbnc.servers.ServerTypeNotFound;
import com.dfbnc.sockets.UnableToConnectException;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.sockets.UserSocketWatcher;
import com.dfbnc.util.Util;
import com.dmdirc.util.io.InvalidConfigFileException;
import uk.org.dataforce.libs.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Functions related to Accounts
 */
public final class Account implements UserSocketWatcher,ConfigChangeListener {

    /** This account name */
    private final String myName;
    /** Is this account an admin */
    private boolean isAdmin;
    /** Deletecode for this account. This is not saved between sessions */
    private String deleteCode = "";
    /** CommandManager for this account */
    private final CommandManager myCommandManager = new CommandManager();
    /** ConnectionHandler for this account */
    private ConnectionHandler myConnectionHandler = null;
    /** ServerLogger for this account */
    private ServerLogger myServerLogger = null;
    /** List of all sockets that are part of this account. */
    private final List<UserSocket> myUserSockets = new CopyOnWriteArrayList<>();
    /** Account config file. */
    private Config config;
    /** SubClient configs. */
    private final Map<String,Config> subClientConfigs = new HashMap<>();
    /** Reverse map of subClientConfigs */
    private final Map<Config,String> subClientConfigKeys = new HashMap<>();
    /** Configuration change listeners. */
    private final Map<String, List<AccountConfigChangeListener>> listeners = new HashMap<>();
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
        Logger.info("Loading Account: " + username);
        final File confDir = getConfigDirectory();
        if (!confDir.exists()) {
            if (!confDir.mkdirs()) {
                throw new IOException("Unable to create config directory.");
            }
        }
        // Load Main Config
        config = new DefaultsConfig(
                new ConfigFileConfig(new File(confDir, username + ".conf")),
                new ConfigFileConfig(DFBnc.class.getResourceAsStream("/com/dfbnc/defaults.config")));
        config.addChangeListener(this);

        // Find sub-client configs
        final File[] subConfigs = confDir.listFiles((final File dir, final String name) -> name.toLowerCase().endsWith(".scconf"));

        // Load Sub-Client Configs
        for (final File sc : subConfigs) {
            // prints file and directory paths
            final String subName = sc.getName().substring(0, sc.getName().lastIndexOf('.'));
            Logger.info("    Found sub-client: " + subName);

            try {
                final Config subConfig = new DefaultsConfig(new ConfigFileConfig(sc), config);
                subConfig.addChangeListener(this);

                subClientConfigs.put(subName, subConfig);
                subClientConfigKeys.put(subConfig, subName);
            } catch (final InvalidConfigFileException icfe) {
                Logger.error("Unable to load sub-client config: " + sc.getName() + "(" + icfe.getMessage() + ")");
            }
         }

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
     * Get the config directory for this account.
     *
     * @return The config directory for this account.
     */
    public File getConfigDirectory() {
        return new File(DFBnc.getConfigDirName(), myName);
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

                    myUserSockets.stream()
                            .filter(socket -> user != socket)
                            .forEach(socket -> socket.sendBotMessage("%s", sb.toString()));
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

        myUserSockets.stream()
                .filter(socket -> user != socket)
                .forEach(socket -> socket.sendBotMessage("%s", sb.toString()));
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
     * Get a List of all UserSockets that are part of this account that are
     * considered "active" clients.
     *
     * @return a List of all UserSockets that are part of this account that are
     *         considered active clients.
     */
    public List<UserSocket> getActiveClientSockets() {
        return myUserSockets.stream().filter(u-> u.isActiveClient()).collect(Collectors.toList());
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

        // Update the ServerLogger
        if (myServerLogger != null) { myServerLogger.disableLogging(); }
        if (handler != null) {
            myServerLogger = handler.getServerLogger();
        }
    }

    /**
     * Save the account settings for this account to the config file
     */
    public void save() {
        config.save();
        subClientConfigs.values().stream().forEach(Config::save);
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
        final StringBuilder hashedPassword = new StringBuilder(myName.toLowerCase());

        hashedPassword.append(password);
        // Append per-client salt if set, else use the old default salt.
        hashedPassword.append(getConfig(subclient).hasOption("user", "salt") ? getConfig(subclient).getOption("user", "salt") : "a5S5l1N4u4O2y9Z4l6W7t1A9b9L8a1X5a7F4s5E8");

        if (checkOldSubClientPassword(subclient, password)) {
            Logger.info("Migrating old subclient password: " + getName() + "+" + subclient);
            config.unsetOption("user", "password." + subclient.toLowerCase());
            getConfig(subclient).setOption("user", "password", Util.md5(hashedPassword.toString()));
        }

        final boolean result = Util.md5(hashedPassword.toString()).equals(getConfig(subclient).getOption("user", "password"));

        // Resalt if using the old default salt.
        if (result && !getConfig(subclient).hasOption("user", "salt")) {
            Logger.info("Re-salting password: " + getName() + "+" + subclient);
            setPassword(subclient, password);
        }

        return result;
    }

    /**
     * Check if an old subclient password matches.
     *
     * @param subclient Subclient to check.
     * @param password Password to check
     * @return true/false depending on successful match
     */
    public boolean checkOldSubClientPassword(final String subclient, final String password) {
        if (subclient == null) { return false; }

        final String passwordKey = "password." + subclient.toLowerCase();
        final StringBuilder hashedPassword = new StringBuilder(myName.toLowerCase());

        if (!config.hasOption("user", passwordKey)) { return false; }

        hashedPassword.append(subclient.toLowerCase());
        hashedPassword.append(password);
        // Old subclient passwords will always use the old default salt.
        hashedPassword.append("a5S5l1N4u4O2y9Z4l6W7t1A9b9L8a1X5a7F4s5E8");

        return Util.md5(hashedPassword.toString()).equals(config.getOption("user", passwordKey));
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
        final StringBuilder hashedPassword = new StringBuilder(myName.toLowerCase());
        final String newSalt = DFBnc.getAccountManager().makePassword(40, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        hashedPassword.append(password);
        hashedPassword.append(newSalt);

        getConfig(subclient).setOption("user", "password", Util.md5(hashedPassword.toString()));
        getConfig(subclient).setOption("user", "salt", newSalt);
        getConfig(subclient).save();
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
            if (myServerLogger != null) { myServerLogger.disableLogging(); }
        }

        final File confDir = new File(DFBnc.getConfigDirName(), getName());
        if (confDir.exists()) {
            if (!Util.deleteFolder(confDir)) {
                throw new IOException("Unable to delete config directory.");
            }
        }

        // Now remove the account.
        DFBnc.getAccountManager().remove(getName());
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
            if (myServerLogger != null) { myServerLogger.disableLogging(); }
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
     * Returns this accounts global config.
     *
     * @return Account's config
     */
    public Config getAccountConfig() {
        return getConfig(null);
    }

    /**
     * Returns the config for the given sub client.
     * If a config does not exist for this subclient, then one will be
     * created. If subName is null or empty, the default config will be
     * returned.
     *
     * @param subName The name of the subclient to get the config for.
     * @return Config for the given subclient.
     */
    public Config getConfig(final String subName) {
        if (subName == null || subName.isEmpty()) { return config; }

        if (!subClientConfigs.containsKey(subName)) {
            Logger.info("Creating new sub-client config for client " + getName() + "+" + subName);
            final File confDir = new File(DFBnc.getConfigDirName(), getName());
            final File sc = new File(confDir, subName + ".scconf");

            try {
                final Config subConfig = new DefaultsConfig(new ConfigFileConfig(sc), config);
                subConfig.addChangeListener(this);

                subClientConfigs.put(subName, subConfig);
                subClientConfigKeys.put(subConfig, subName);
            } catch (final InvalidConfigFileException icfe) {
                Logger.error("Unable to load sub-client config: " + sc.getName() + "(" + icfe.getMessage() + ")");
                icfe.printStackTrace();
            } catch (final IOException ioe) {
                // This should hopefully never happen.
                // We're in trouble if it does.
                Logger.error("Error loading sub-client config: " + sc.getName() + "(" + ioe.getMessage() + ")");
                ioe.printStackTrace();
            }
        }

        return subClientConfigs.get(subName);
    }

    /**
     * Get all the subclient Configs
     *
     * @return sub client configs.
     */
    public Map<String,Config> getSubClientConfigs() {
        return subClientConfigs;
    }

    /**
     * Adds a change listener for the specified domain.
     *
     * @param domain The domain to be monitored
     * @param listener The listener to register
     */
    public void addConfigChangeListener(final String domain, final AccountConfigChangeListener listener) {
        addConfigListener(domain, listener);
    }

    /**
     * Adds a change listener for all domains.
     *
     * @param listener The listener to register
     */
    public void addConfigChangeListener(final AccountConfigChangeListener listener) {
        addConfigListener("", listener);
    }

    /**
     * Adds a change listener for the specified domain and key.
     *
     * @param domain The domain of the option
     * @param key The option to be monitored
     * @param listener The listener to register
     */
    public void addConfigChangeListener(final String domain, final String key, final AccountConfigChangeListener listener) {
        addConfigListener(domain + "." + key, listener);
    }

    /**
     * Adds a change listener to the list of listeners.
     *
     * @param key the key to listen for.
     * @param listener The listener to register
     */
    protected void addConfigListener(final String key, final AccountConfigChangeListener listener) {
        if (!listeners.containsKey(key)) {
            final List<AccountConfigChangeListener> l = new ArrayList<>();
            listeners.put(key, l);
        }
        if (!listeners.get(key).contains(listener)) {
            listeners.get(key).add(listener);
        }
    }

    /**
     * Removes the specified listener for all domains and options.
     *
     * @param listener The listener to be removed
     */
    public void removeListener(final AccountConfigChangeListener listener) {
        listeners.values().forEach(list -> list.remove(listener));
    }

    /**
     * Call matching listeners.
     *
     * @param config The config object that had the change
     * @param domain the domain that changed
     * @param option the option that changed
     */
    @Override
    public void configChanged(final Config config, final String domain, final String option) {
        final String subClientName = (config == this.config) ? null : subClientConfigKeys.get(config);

        if (listeners.containsKey(domain)) {
            listeners.get(domain).forEach(listener -> listener.accountConfigChanged(this, subClientName, domain, option));
        }
        if (listeners.containsKey(domain + "." + option)) {
            listeners.get(domain + "." + option).forEach((listener) -> listener.accountConfigChanged(this, subClientName, domain, option));
        }
        if (listeners.containsKey("")) {
            listeners.get("").forEach((listener) -> listener.accountConfigChanged(this, subClientName, domain, option));
        }
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
     * Called when the IRC Connection handler is disconnected.
     *
     * @param reason Reason for disconnection
     */
    public void handlerDisconnected(final String reason) {
        final ConnectionHandler oldHandler = myConnectionHandler;
        if (myServerLogger != null) { myServerLogger.disableLogging(); }

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
                        sendBotMessage("Unable to reconnect: %s", ex.getMessage());
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
                    socket.sendBotMessage("Disconnected from server: %s", reason);
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
     * Report an exception to the console and any connected user only if
     * server.reporterrors is set to true.
     *
     * @param t Throwable to report.
     * @param type Human-Friendly type of exception.
     */
    public void reportException(final Throwable t, final String type) {
        if (!getAccountConfig().getOptionBool("server", "reporterrors") || t == null) {
            return;
        }

        forceReportException(t, type);
    }

    /**
     * Report an exception to the console and any connected user, regardless of
     * the value of server.reporterrors.
     *
     * @param t Throwable to report.
     * @param type Human-Friendly type of exception.
     */
    public void forceReportException(final Throwable t, final String type) {
        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        Logger.error("----");
        Logger.error(type + ": " + t);
        Logger.error(type + " Stack trace: " + writer.getBuffer());
        Logger.error("----");

        sendBotMessage("----");
        sendBotMessage("%s: %s", type, t);
        sendBotMessage("%s Stack trace:", type);
        final String[] bits = writer.getBuffer().toString().split("\n");
        for (final String bit : bits) {
            sendBotMessage("    %s", bit);
        }
        sendBotMessage("----");
    }
}

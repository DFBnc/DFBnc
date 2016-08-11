package com.dfbnc.authentication;

import com.dfbnc.Account;
import com.dfbnc.AccountManager;
import com.dfbnc.Consts;
import com.dfbnc.DFBnc;
import com.dfbnc.sockets.UserSocket;

import uk.org.dataforce.libs.logger.Logger;

/**
 * Manages the authentication process for a user.
 */
public class Authenticator {

    /**
     * Describes the current status of an authenticator.
     */
    public enum Status {
        /** The authenticator is waiting for the initial NICK and USER commands. */
        WAITING_FOR_NICK_OR_USER,
        /** The authenticator is waiting for a PASS command. */
        WAITING_FOR_PASS,
        /** The authenticator is ready to authenticate. */
        READY,
        /** The authenticator is done and no more work is needed. */
        FINISHED,
    }

    private final AccountManager accountManager;
    private final UserSocket userSocket;

    private String nickname;
    private String realname;
    private String usernameFromPass;
    private String usernameFromUser;
    private String password;
    private String subclient;
    private String clientType;

    private boolean needPassword = true;

    // TODO: make this a config setting
    private int remainingPasswordTries = 3;
    private boolean success;

    /**
     * Creates a new authenticator for the given socket.
     *
     * @param accountManager The account manager to use to lookup users.
     * @param userSocket The socket that requires authentication.
     */
    public Authenticator(final AccountManager accountManager, final UserSocket userSocket) {
        this.accountManager = accountManager;
        this.userSocket = userSocket;
    }

    /**
     * Handles a 'NICK' command.
     *
     * @param nickname The nickname passed to the socket.
     */
    public void handleNickCommand(final String nickname) {
        this.nickname = nickname;
    }

    /**
     * Handles a 'USER' command.
     *
     * @param username The username passed to the socket.
     * @param realname The real name passed to the socket.
     */
    public void handleUserCommand(final String username, final String realname) {
        this.usernameFromUser = username;
        this.realname = realname;
        isPasswordRequired(username);
    }

    /**
     * Handles a 'PASS' command.
     *
     * @param password The password passed to the socket.
     */
    public void handlePassCommand(final String password) {
        if (password.contains(":")) {
            // If the password contains a : then we know it contains both a username and a password
            final String[] parts = password.split(":", 2);
            this.usernameFromPass = parts[0];
            this.password = parts[1];
            return;
        }

        // Try treating it as a client (+ optional subclient).
        if (!isPasswordRequired(password)) {
            this.usernameFromPass = password;
            return;
        }

        // Otherwise, assume that the password is just a password(!)
        this.password = password;
    }

    /**
     * Checks whether a password would be required for the given username. A password isn't required if the
     * client/subclient has an alternative auth mechanism in its authlist, and such mechanism succeeds.
     *
     * @param username The username to check password requirements for.
     * @return True if a password is required (authlist auth failed), false if not (authlist auth succeeded).
     */
    private boolean isPasswordRequired(final String username) {
        final String[] clientParts = splitUsername(username);
        if (accountManager.exists(clientParts[0])) {
            final Account account = accountManager.get(clientParts[0]);
            if (account.isAuthenticated(userSocket, clientParts[1], false)) {
                // We don't need an actual password.
                this.needPassword = false;
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the current status of the authenticator (whether it's waiting for more input or ready to authenticate).
     *
     * @return The current status of the authenticator.
     */
    public Status getStatus() {
        if (this.nickname == null || this.realname == null) {
            return Status.WAITING_FOR_NICK_OR_USER;
        }

        if (this.password == null && this.needPassword) {
            return Status.WAITING_FOR_PASS;
        }

        if (this.remainingPasswordTries == 0 || this.success) {
            return Status.FINISHED;
        }

        return Status.READY;
    }

    /**
     * Returns the name of the subclient that was authenticated.
     *
     * @return The subclient in use (or null).
     */
    public String getSubClient() {
        return subclient;
    }

    /**
     * Returns the client type specified by the user, if any.
     *
     * @return The client type specified (or null).
     */
    public String getClientType() {
        return clientType;
    }

    /**
     * Attempts to authenticate the connection.
     *
     * <p>Should only be called when {@link #getStatus()} indicates that the authenticator is {@link Status#READY}.
     *
     * <p>If the user authenticates successfully, the corresponding {@link Account} is returned; otherwise {@code null}
     * will be returned. The value of {@link #getStatus()} will indicate whether any further information is required
     * (e.g. a password retry).
     *
     * @param responseCommand The command the user entered that this authentication attempt was triggered by; used
     *                        in error responses.
     * @return An {@link Account} if auth was successful, {@code null} otherwise.
     */
    public Account authenticate(final String responseCommand) {
        final String[] clientParts = splitUsername(usernameFromPass == null ? usernameFromUser : usernameFromPass);

        handleAutoAccountCreation(clientParts[0]);

        if (accountManager.exists(clientParts[0])) {
            final Account account = accountManager.get(clientParts[0]);
            if (account.checkAuthentication(userSocket, clientParts[1], password)) {
                this.subclient = clientParts[1];
                this.clientType = clientParts[2];

                return handleSuccessfulAuth(account);
            } else {
                handleInvalidPassword(responseCommand);
            }
        } else {
            handleInvalidPassword(responseCommand);
        }

        return null;
    }

    /**
     * Handles automatic creation of accounts if the user is the first one to connect, or if auto-create is enabled
     * and the account doesn't exist.
     */
    private void handleAutoAccountCreation(final String client) {
        if (password == null) {
            // Don't allow account creation without passwords...
            return;
        }

        final DFBnc bnc = DFBnc.getBNC();
        if (accountManager.count() == 0 || (bnc.allowAutoCreate() && !accountManager.exists(client))) {
            Account acc = accountManager.createAccount(client, password);
            if (accountManager.count() == 1) {
                acc.setAdmin(true);
                userSocket.sendBotMessage("You are the first user of this bnc, and have been made an admin.");
            } else {
                userSocket.sendBotMessage("The given account does not exist, so an account has been created for you.");
            }
            accountManager.saveAccounts();
            bnc.getConfig().save();
        }
    }

    /**
     * Handles a successful authentication attempt.
     */
    private Account handleSuccessfulAuth(Account account) {
        if (account.isSuspended()) {
            userSocket.sendBotMessage("This account has been suspended.");
            userSocket.sendBotMessage("Reason: %s", account.getSuspendReason());
            userSocket.close("Account suspended.");
            return null;
        }

        success = true;
        userSocket.sendBotMessage("You are now logged in");
        if (account.isAdmin()) {
            userSocket.sendBotMessage("This is an Admin account");
        }

        Logger.debug2("processNonAuthenticated - User Connected");
        account.userConnected(userSocket);
        Logger.debug2("userConnected finished");
        return account;
    }

    /**
     * Deals with the user providing an invalid password.
     *
     * @param lastCommand The command the user last attempted, used in error responses.
     */
    private void handleInvalidPassword(final String lastCommand) {
        remainingPasswordTries--;
        final StringBuilder message = new StringBuilder("Password incorrect, or account not found.");
        message.append(" You have ")
                .append(remainingPasswordTries)
                .append(" attempt")
                .append(remainingPasswordTries == 1 ? "" : "s")
                .append(" left.");

        userSocket.sendIRCLine(Consts.ERR_PASSWDMISMATCH, lastCommand, message.toString());
        userSocket.sendBotMessage("%s", message.toString());

        if (remainingPasswordTries == 0) {
            userSocket.sendIRCLine(Consts.ERR_PASSWDMISMATCH, lastCommand, "Too many password attempts, closing socket.");
            userSocket.sendBotMessage("Too many password attempts, closing socket.");
            userSocket.close("Too many password attempts.");
        } else {
            password = null;
        }
    }

    /**
     * Splits a username into a client, subclient and client type.
     *
     * @param username The username to be split.
     * @return An array containing three entries - the client, the (possibly null) subclient, and the (possibly null)
     *         client type.
     */
    private static String[] splitUsername(final String username) {
        final String[] res = {null, null, null};

        final String[] clientParts = username.split("\\+");
        res[0]  = clientParts[0];

        if (clientParts.length >= 2 && !clientParts[1].isEmpty()) {
            res[1] = clientParts[1].replaceAll("[^a-z0-9_-]", "");
        }

        if (clientParts.length >= 3) {
            res[2] = clientParts[2];
        }

        return res;
    }

}

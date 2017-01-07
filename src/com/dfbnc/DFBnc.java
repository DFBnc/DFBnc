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

import com.dfbnc.authentication.AuthProviderManager;
import com.dfbnc.authentication.providers.*;
import com.dfbnc.commands.CommandManager;
import com.dfbnc.commands.admin.*;
import com.dfbnc.commands.user.*;
import com.dfbnc.config.Config;
import com.dfbnc.config.ConfigFileConfig;
import com.dfbnc.config.DefaultsConfig;
import com.dfbnc.config.ReadOnlyConfig;
import com.dfbnc.servers.ServerTypeManager;
import com.dfbnc.sockets.NewSocketReadyHandler;
import com.dfbnc.sockets.ListenSocket;
import com.dfbnc.sockets.UserSocket;
import com.dfbnc.sockets.secure.SSLContextManager;
import com.dfbnc.util.MultiWriter;
import com.dfbnc.util.RollingWriter;
import com.dmdirc.util.io.InvalidConfigFileException;
import uk.org.dataforce.libs.cliparser.BooleanParam;
import uk.org.dataforce.libs.cliparser.CLIParam;
import uk.org.dataforce.libs.cliparser.CLIParser;
import uk.org.dataforce.libs.cliparser.StringParam;
import uk.org.dataforce.libs.logger.LogLevel;
import uk.org.dataforce.libs.logger.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main BNC Class.
 */
public class DFBnc implements NewSocketReadyHandler {
    /** Me */
    private static DFBnc me = null;

    /** Version Config File */
    private static Config versionConfig;

    /** The CLIParser */
    private static CLIParser cli = CLIParser.getCLIParser();

    /** The config directory file name */
    private static String configDirectory = "DFBnc";

    /** The config file name */
    private static String configFile = "DFBnc.conf";

    /** The user command manager for this bnc */
    private static CommandManager userCommandManager = new CommandManager();

    /** The admin command manager for this bnc */
    private static CommandManager adminCommandManager = new CommandManager();

    /** The ServerType manager for this bnc */
    private static ServerTypeManager myServerTypeManager = new ServerTypeManager();

    /** The account manager for this bnc */
    private static AccountManager accountManager = new AccountManager();

    /** The provider manager for this bnc */
    private static AuthProviderManager authProviderManager = new AuthProviderManager();

    /** The arraylist of listenSockets */
    private static ArrayList<ListenSocket> listenSockets = new ArrayList<>();

    /** The time that the BNC was started at */
    public static final Long startTime = System.currentTimeMillis();

    /** Global config. */
    private Config config;

    /** Shutdown hook. */
    private ShutdownHook shutdownHook;

    /** Daemon. */
    public final static DFBncDaemon daemon = new DFBncDaemon();

    /** PID File name. */
    static String pidFile = "";

    /** MultiWriter for logger. */
    final MultiWriter multiWriter = new MultiWriter();

    /** Store log entries here to let the show logging command work. */
    final RollingWriter rollingWriter = new RollingWriter(1000);

    /** Log File Writer. */
    Writer logFileWriter;

    /** Our SSLContextManager. */
    private SSLContextManager sslContextManager;
    
    /**
     * Create the BNC.
     */
    private DFBnc() { }

    /**
     * Init the application.
     * Parses CLI Arguments, loads config file, and sets up the listen sockets.
     *
     * @param args CLI Arguments passed to application
     */
    private void init(final String[] args) {
        Logger.setWriter(multiWriter);
        multiWriter.addWriter(rollingWriter);
        Logger.setLevel(LogLevel.INFO);

        loadVersionInfo();

        if (DFBncDaemon.canFork() && daemon.isDaemonized()) {
            Logger.setTag("(" + DFBncDaemon.getPID() + ") Child");
        } else {
            Logger.info("Starting DFBnc (Version: " + getVersion() + ")..");
        }

        setupCLIParser();
        if (cli.wantsHelp(args)) {
            cli.showHelp("DFBnc Help", "DFBnc [options]");
            System.exit(0);
        }

        cli.parseArgs(args, true);

        if (cli.paramGiven("-version")) {
            for (Entry<String, String> e : DFBnc.getVersions().entrySet()) {
                Logger.info(e.getKey() + " version: " + e.getValue());
            }

            System.exit(0);
        }

        Logger.info("Adding shutdown hook");
        shutdownHook = new ShutdownHook(this);
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        setupLogging();

        if (DFBncDaemon.canFork() && daemon.isDaemonized()) {
            try {
                final CLIParam pidFileCLI = cli.getParam("-pidfile");
                pidFile = pidFileCLI.getStringValue().isEmpty() ? "dfbnc.pid" : pidFileCLI.getStringValue();
                Logger.info("Using pid file: " + pidFile);

                daemon.init(pidFile);
            } catch (final Exception e) {
                Logger.error("Daemon init failed. Exiting: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        } else if (!DFBncDaemon.canFork() && !cli.paramGiven("-foreground")) {
            Logger.error("Forking is not possible on the current OS (" +  System.getProperty("os.name") + ").");
        } else if (DFBncDaemon.canFork() && !cli.paramGiven("-foreground")) {
            try {
                Logger.info("Forking to background...");
                Logger.info(null);

                // Before forking, close any sockets and files.
                Logger.setLevel(LogLevel.SILENT);
                shutdownHook.inactivate();
                this.shutdown(true);

                // Daemonise.
                daemon.daemonize();

                // Wait a short while for child to start so that user can see
                // its output without a shell prompt appearing!
                Thread.sleep(2000);

                // Exit the parent.
                System.exit(0);
            } catch (Throwable t) {
                Logger.error("Forking failed: " + t);
                t.printStackTrace();
                System.exit(1);
            }
        }

        if (cli.paramGiven("-silent")) {
            Logger.setLevel(LogLevel.SILENT);
        } else if (cli.getParamNumber("-debug") >= 9) {
            Logger.info("Enabling Stupidly Advanced Debugging Information (DEBUG9).");
            Logger.setLevel(LogLevel.DEBUG9);
        } else if (cli.getParamNumber("-debug") == 8) {
            Logger.info("Enabling Spammy Advanced Debugging Information (DEBUG8).");
            Logger.setLevel(LogLevel.DEBUG8);
        } else if (cli.getParamNumber("-debug") == 7) {
            Logger.info("Enabling Stupid Amounts of Advanced Debugging Information (DEBUG7).");
            Logger.setLevel(LogLevel.DEBUG7);
        } else if (cli.getParamNumber("-debug") == 6) {
            Logger.info("Enabling Loads More Advanced Debugging Information (DEBUG6).");
            Logger.setLevel(LogLevel.DEBUG6);
        } else if (cli.getParamNumber("-debug") == 5) {
            Logger.info("Enabling Yet More Advanced Debugging Information (DEBUG5).");
            Logger.setLevel(LogLevel.DEBUG5);
        } else if (cli.getParamNumber("-debug") == 4) {
            Logger.info("Enabling Even More Advanced Debugging Information (DEBUG4).");
            Logger.setLevel(LogLevel.DEBUG4);
        } else if (cli.getParamNumber("-debug") == 3) {
            Logger.info("Enabling More Advanced Debugging Information (DEBUG3).");
            Logger.setLevel(LogLevel.DEBUG3);
        } else if (cli.getParamNumber("-debug") == 2) {
            Logger.info("Enabling Advanced Debugging Information (DEBUG2).");
            Logger.setLevel(LogLevel.DEBUG2);
        } else if (cli.getParamNumber("-debug") == 1) {
            Logger.info("Enabling Debugging Information (DEBUG).");
            Logger.setLevel(LogLevel.DEBUG);
        }

        if (cli.paramGiven("-config")) { configDirectory = cli.getParam("-config").getStringValue(); }
        Logger.info("Loading Config..");

        try {
            final File directory = new File(getConfigDirName());
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Unable to create config directory.");
                }
            }
            config = new DefaultsConfig(new ConfigFileConfig(new File(getConfigDirName(), getConfigFileName())), new ConfigFileConfig(DFBnc.class.getResourceAsStream("/com/dfbnc/defaults.config")));
        } catch (final IOException ex) {
            Logger.error("Error loading config: " + configDirectory + " (" + ex.getMessage() + "). Exiting");
            System.exit(1);
        } catch (final InvalidConfigFileException ex) {
            Logger.error("Error loading config (" + ex.getMessage() + "). Exiting");
            System.exit(1);
        }

        // Now that we have a config file, set the log buffer capacity correctly.
        rollingWriter.setCapacity(getConfig().getOptionInt("general", "logBuffer"));

        Logger.info("Setting up Default User Command Manager");
        userCommandManager.addCommand(new ServerTypeCommand(userCommandManager));
        userCommandManager.addCommand(new ShowCommand(userCommandManager));
        userCommandManager.addCommand(new DebugCommand(userCommandManager));
        userCommandManager.addCommand(new HelpCommand(userCommandManager));
        userCommandManager.addCommand(new PasswordCommand(userCommandManager));
        userCommandManager.addCommand(new AuthListCommand(userCommandManager));
        userCommandManager.addCommand(new SaveCommand(userCommandManager));
        userCommandManager.addCommand(new ServerSetCommand(userCommandManager));
        userCommandManager.addCommand(new UserSetCommand(userCommandManager));
        userCommandManager.addCommand(new ConnectCommand(userCommandManager));

        Logger.info("Setting up Default Admin Command Manager");
        adminCommandManager.addCommand(new AddUserCommand(adminCommandManager));
        adminCommandManager.addCommand(new DelUserCommand(adminCommandManager));
        adminCommandManager.addCommand(new SuspendCommand(adminCommandManager));
        adminCommandManager.addCommand(new UnsuspendCommand(adminCommandManager));
        adminCommandManager.addCommand(new SetAdminCommand(adminCommandManager));
        adminCommandManager.addCommand(new ShutdownCommand(adminCommandManager));
        adminCommandManager.addCommand(new DebugCommand(adminCommandManager));

        Logger.info("Setting up AuthProvider Manager");
        authProviderManager.addProvider(new ClientCertProvider());

        Logger.info("Setting up ServerType Manager");
        myServerTypeManager.init();

        Logger.info("Running!");
        if (allowAutoCreate()) {
            Logger.warning(".-----------------------------------------------------,");
            Logger.warning("|                       WARNING                       |");
            Logger.warning("|-----------------------------------------------------|");
            Logger.warning("| AutoCreate mode is enabled!                         |");
            Logger.warning("|                                                     |");
            Logger.warning("| This mode makes the BNC automatiaclly create new    |");
            Logger.warning("| accounts for unknown users and should not be used   |");
            Logger.warning("| in a non-test environment.                          |");
            Logger.warning("`-----------------------------------------------------'");
        }

        // By now, we will have forked if required.
        Logger.info("Loading Accounts..");
        accountManager.loadAccounts();

        openListenSockets();

        // Check UserSockets every FREQUENCY seconds for inactivity, with a
        // threshold of THRESHOLD.
        // This will cause sockets to send an initial PING once the threshold has been hit
        final Timer socketChecker = new Timer("Socket Checker Timer", true);
        final int pingThreshold = config.getOptionInt("timeout", "threshold");
        final int pingFrequency = config.getOptionInt("timeout", "frequency") * 1000;

        socketChecker.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                UserSocket.checkAll(pingThreshold);
            }
        }, pingFrequency, pingFrequency);

        if (DFBncDaemon.canFork() && daemon.isDaemonized()) {
            Logger.info("Forked and running! (PID: " + DFBncDaemon.getPID() +")");
            try {
                daemon.closeDescriptors();
            } catch (final IOException ioe) {
                Logger.error("Error closing file descriptors: " + ioe);
                ioe.printStackTrace();
            }
        } else {
            Logger.info("Running!");
        }
    }

    /**
     * Set up the log file.
     */
    public void setupLogging() {
        final CLIParam logFile = cli.getParam("-logfile");
        if (!logFile.getStringValue().isEmpty()) {
            final File file = new File(logFile.getStringValue());
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (final IOException ex) {
                    Logger.error("Unable to create log file: " + ex);
                }
            }

            try {
                final BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (!Logger.getTag().isEmpty()) {
                    bw.append("[");
                    bw.append(Logger.getTag());
                    bw.append("] ");
                }
                bw.append("Log file opened at: ").append(sdf.format(new Date(System.currentTimeMillis())));
                bw.append("\n");
                bw.flush();
                // We will never get to setting it here if it failed to write above!
                logFileWriter = bw;
                multiWriter.addWriter(logFileWriter);
                Logger.info("Using log file: " + file);
            } catch (final IOException ex) {
                Logger.error("Unable to write to log file: " + ex);
            }
        }
    }

    /**
     * Open the listen sockets.
     */
    public void openListenSockets() {
        Logger.info("Opening Listen Sockets..");
        int count = 0;
        final List<String> listenhosts = config.getOptionList("general", "listenhost");

        if (sslContextManager == null) {
            sslContextManager = new SSLContextManager(getConfig().getOption("ssl", "keystore"), getConfig().getOption("ssl", "storepass"), getConfig().getOption("ssl", "keypass"));
        }
        
        for (String listenhost : listenhosts) {
            try {
                listenSockets.add(new ListenSocket(listenhost, this, sslContextManager));
                count++;
            } catch (IOException e) {
                Logger.error("Unable to open socket (" + listenhost +"): "+e.getMessage());
            }
        }
        if (count == 0) {
            Logger.info("No sockets could be opened, Terminating");
            System.exit(1);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void handleNewSocketReady(final SocketChannel sChannel, final SSLContextManager newSocketSSLContextManager) throws IOException {
        new UserSocket(sChannel, newSocketSSLContextManager).socketOpened();
    }

    /**
     * Load the version info from the jar file if present.
     */
    public static void loadVersionInfo() {
        final InputStream version = DFBnc.class.getResourceAsStream("/com/dfbnc/version.config");
        if (version != null) {
            try {
                versionConfig = new ReadOnlyConfig(new ConfigFileConfig(version));
            } catch (final Exception e) { /** Oh well, default it is. */ }
        }
    }

    /**
     * Get the DFBNC Version if possible.
     *
     * @return DFBnc Version.
     */
    public static String getVersion() {
        return getVersion("dfbnc");
    }

    /**
     * Get the Version of a given component if possible.
     *
     * @param component Component to get version for.
     * @return Component Version.
     */
    public static String getVersion(final String component) {
        if (versionConfig == null) {
            return "Unknown";
        }
        return versionConfig.hasOption("versions", component)
                ? versionConfig.getOption("versions", component)
                : "Unknown";
    }

    /**
     * Get the versions of all known components.
     *
     * @return Component Versions Map.
     */
    public static Map<String,String> getVersions() {
        if (versionConfig == null) {
            return Collections.emptyMap();
        }
        return versionConfig.getOptions("versions");
    }

    /**
     * Get the start time
     *
     * @return the start time.
     */
    public static long getStartTime() {
        return startTime;
    }

    /**
     * Get the rolling log.
     *
     * @return the rolling writer used for log files.
     */
    public RollingWriter getRollingWriter() {
        return rollingWriter;
    }

    /**
     * Get the multiwriter.
     *
     * @return the multi writer used for log files.
     */
    public MultiWriter getMultiWriter() {
        return multiWriter;
    }

    /**
     * Handle shutdown
     */
    public void shutdown() {
        shutdown(false);
    }

    /**
     * Handle shutdown
     * @param shuttingDown are we already shutting down?
     */
    public void shutdown(final boolean shuttingDown) {
        Logger.info("---------------------");
        Logger.info("Shuting down.");

        Logger.info("Closing Listen Sockets");
        for (final ListenSocket ls : listenSockets) {
            ls.close();
        }
        listenSockets.clear();

        Logger.info("Closing User Sockets");
        UserSocket.closeAll("BNC Shutdown");

        Logger.info("Saving Accounts");
        accountManager.shutdown();
        accountManager.saveAccounts();

        if (config != null) {
            Logger.info("Saving config to '"+configFile+"'");
            config.save();
        }

        if (DFBncDaemon.canFork() && daemon.isDaemonized()) {
            if (!pidFile.isEmpty()) {
                Logger.info("Removing pid file");
                final File pid = new File(pidFile);
                if (pid.exists()) { pid.delete(); }
            }
        }

        final Writer bw = logFileWriter;
        if (bw != null) {
            Logger.info("Closing log file");
            multiWriter.removeWriter(logFileWriter);
            try {
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (!Logger.getTag().isEmpty()) {
                    bw.append("[");
                    bw.append(Logger.getTag());
                    bw.append("] ");
                }
                bw.append("Log file closed at: ").append(sdf.format(new Date(System.currentTimeMillis())));
                bw.append("\n");
                bw.flush();
                bw.close();
                logFileWriter = null;
            } catch (final IOException ioe) { /** Oh well. */ }
        }

        Logger.info("Deactivating shutdown hook.");
        if (shutdownHook != null) { shutdownHook.inactivate(); }
        if (!shuttingDown) {
            Logger.info("Exiting.");
            System.exit(0);
        }
    }

    /**
     * Get the name of the configfile
     *
     * @return The name of the configfile
     */
    public static String getConfigDirName() {
        return configDirectory;
    }

    /**
     * Get the name of the configfile
     *
     * @return The name of the configfile
     */
    public static String getConfigFileName() {
        return configFile;
    }

    /**
     * Get the user CommandManager
     *
     * @return The user CommandManager
     */
    public static CommandManager getUserCommandManager() {
        return userCommandManager;
    }

    /**
     * Get the admin CommandManager
     *
     * @return The admin CommandManager
     */
    public static CommandManager getAdminCommandManager() {
        return adminCommandManager;
    }

    /**
     * Get the ServerTypeManager
     *
     * @return The ServerTypeManager
     */
    public static ServerTypeManager getServerTypeManager() {
        return myServerTypeManager;
    }

    /**
     * Get the AccountManager
     *
     * @return The AccountManager
     */
    public static AccountManager getAccountManager() {
        return accountManager;
    }

    /**
     * Get the AuthProviderManager
     *
     * @return The AuthProviderManager
     */
    public static AuthProviderManager getAuthProviderManager() {
        return authProviderManager;
    }

    /**
     * Get the listenSockets array list
     *
     * @return The name of the configfile
     */
    public static ArrayList<ListenSocket> getListenSockets() {
        return listenSockets;
    }

    /**
     * Setup the cli parser.
     * This clears the current CLIParser params and creates new ones.
     */
    private static void setupCLIParser() {
        cli.clear();
        cli.add(new BooleanParam('h', "help", "Show Help"));
        cli.add(new BooleanParam('v', "version", "Show version information."));
        cli.add(new BooleanParam('d', "debug", "Enable extra debugging. (Use multiple times for more)"));
        cli.add(new BooleanParam('s', "silent", "Disable all output"));
        cli.add(new StringParam('c', "config", "Alternative config directory to use"));
        cli.add(new BooleanParam((char)0, "enableDebugOptions", "Enable 'debugging.*' config settings"));
        cli.add(new BooleanParam('f', "foreground", "Don't Fork into background"));
        if (DFBncDaemon.canFork()) {
            cli.add(new BooleanParam((char)0, "background", "Exists for backwards compatability..."));
        } else {
            cli.add(new BooleanParam((char)0, "background", "Exists for backwards compatability... [UNSUPPORTED ON THIS OS]"));
        }
        cli.add(new StringParam((char)0, "pidfile", "Change pidfile location (Default: ./dfbnc.pid)"));
        cli.add(new StringParam((char)0, "logfile", "Log file to use for console output. (Default: none)"));
        cli.setHelp(cli.getParam("-help"));
    }

    /**
     * Get the DFBnc instance.
     *
     * @return the DFBnc instance.
     */
    public static DFBnc getBNC() {
        return me;
    }

    /**
     * Returns the global config.
     *
     * @return Global config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Get the default server name to use.
     *
     * @return Default server name to use.
     */
    public String getDefaultServerName() {
        return getConfig().getOption("general", "ServerName");
    }

    /**
     * Start the application.
     *
     * @param args CLI Arguments passed to application
     */
    public static void main(String[] args) {
        me = new DFBnc();
        me.init(args);
    }

    /**
     * Are we allowing autocreation of users?
     *
     * @return True if autocreate is enabled.
     */
    public boolean allowAutoCreate() {
        return cli.paramGiven("-enableDebugOptions") && config.getOptionBool("debugging", "autocreate");
    }
}

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

import com.dmdirc.util.InvalidConfigFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.commands.admin.*;
import uk.org.dataforce.dfbnc.commands.user.*;
import uk.org.dataforce.dfbnc.servers.ServerTypeManager;
import uk.org.dataforce.libs.cliparser.BooleanParam;
import uk.org.dataforce.libs.cliparser.CLIParser;
import uk.org.dataforce.libs.cliparser.StringParam;
import uk.org.dataforce.libs.logger.LogLevel;
import uk.org.dataforce.libs.logger.Logger;

/**
 * Main BNC Class.
 */
public class DFBnc {
    /** Me */
    private static DFBnc me = null;

    /** Version String */
    public final static String VERSION = "DFBnc-Java 0.1";
    
    /** The CLIParser */
    private static CLIParser cli = CLIParser.getCLIParser();

    /** The config file name */
    private static String configDirectory = "DFBnc";
    
    /** The config file name */
    private static String configFile = "DFBnc.conf";
    
    /** The user command manager for this bnc */
    private static CommandManager userCommandManager = new CommandManager();
    
    /** The admin command manager for this bnc */
    private static CommandManager adminCommandManager = new CommandManager();
    
    /** The ServerType manager for this bnc */
    private static ServerTypeManager myServerTypeManager = new ServerTypeManager();
    
    /** The arraylist of listenSockets */
    private static ArrayList<ListenSocket> listenSockets = new ArrayList<ListenSocket>();

    /** The time that the BNC was started at */
    private static Long startTime = System.currentTimeMillis();

    /** Global config. */
    private Config config;

    /**
     * Run the application.
     * Parses CLI Arguments, loads config file, and sets up the listen sockets.
     *
     * @param args CLI Arguments passed to application
     */
    private DFBnc(String[] args) {
        Logger.setLevel(LogLevel.INFO);
        Logger.info("Starting DFBnc..");
        setupCLIParser();
        if (cli.wantsHelp(args)) {
            cli.showHelp("DFBnc Help", "DFBnc [options]");
            System.exit(0);
        }
        
        cli.parseArgs(args, true);
        
        if (cli.getParamNumber("-silent") > 0) {
            Logger.setLevel(LogLevel.SILENT);
        } else if (cli.getParamNumber("-debug") > 1) {
            Logger.info("Enabling Extra Debugging Information.");
            Logger.setLevel(LogLevel.DEBUG2);
        } else if (cli.getParamNumber("-debug") > 0) {
            Logger.info("Enabling Debugging Information.");
            Logger.setLevel(LogLevel.DEBUG);
        }
        
        if (cli.getParamNumber("-config") > 0) { configFile = cli.getParam("-config").getStringValue(); }
        Logger.info("Loading Config..");
        try {
            config = createDefaultConfig();
        } catch (IOException ex) {
            Logger.error("Error loading config (" + ex.getMessage() + "). Exiting");
            System.exit(0);
        } catch (InvalidConfigFileException ex) {
            Logger.error("Error loading config (" + ex.getMessage() + "). Exiting");
            System.exit(0);
        }
        
        Logger.info("Setting up Default User Command Manager");
        userCommandManager.addCommand(new VersionCommand(userCommandManager));
        userCommandManager.addCommand(new FirstTimeCommand(userCommandManager));
        userCommandManager.addCommand(new ServerTypeCommand(userCommandManager));
        userCommandManager.addCommand(new ShowCommandsCommand(userCommandManager));
        userCommandManager.addCommand(new HelpCommand(userCommandManager));
        userCommandManager.addCommand(new PasswordCommand(userCommandManager));
        
        Logger.info("Setting up Default Admin Command Manager");
        adminCommandManager.addCommand(new AddUserCommand(adminCommandManager));
        adminCommandManager.addCommand(new DelUserCommand(adminCommandManager));
        adminCommandManager.addCommand(new SuspendCommand(adminCommandManager));
        adminCommandManager.addCommand(new UnsuspendCommand(adminCommandManager));
        adminCommandManager.addCommand(new SetAdminCommand(adminCommandManager));
        
        Logger.info("Setting up ServerType Manager");
        myServerTypeManager.init();
        
        Logger.info("Loading Accounts..");
        AccountManager.loadAccounts();
        
        Logger.info("Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
        
        Logger.info("Opening Listen Sockets..");
        int count = 0;
        List<String> defaulthosts = new ArrayList<String>();
        defaulthosts.add("0.0.0.0:33262");
        defaulthosts.add("0.0.0.0:+33263");
        List<String> listenhosts = config.getListOption("general", "listenhost", defaulthosts);
        
        for (String listenhost : listenhosts) {
            try {
                listenSockets.add(new ListenSocket(listenhost));
                count++;
            } catch (IOException e) {
                Logger.error("Unable to open socket: "+e.getMessage());
            }
            if (count == 0) {
                Logger.info("No sockets could be opened, Terminating");
                System.exit(0);
            }
        }
        Logger.info("Running!");
        if (config.getBoolOption("debugging", "autocreate", false)) {
            Logger.warning("/-----------------------------------------------------\\");
            Logger.warning("|                       WARNING                       |");
            Logger.warning("|-----------------------------------------------------|");
            Logger.warning("| AutoCreate mode is enabled!                         |");
            Logger.warning("|                                                     |");
            Logger.warning("| This mode makes the BNC automatiaclly create new    |");
            Logger.warning("| accounts for unknown users and should not be used   |");
            Logger.warning("| in a non-test environment.                          |");
            Logger.warning("`-----------------------------------------------------'");
        }
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
     * Handle shutdown
     */
    public void shutdown() {
        Logger.info("---------------------");
        Logger.info("Shuting down.");
        
        Logger.info("Closing Listen Sockets");
        for (int i = 0; i < listenSockets.size() ; ++i) {
            final ListenSocket ls = listenSockets.get(i);
            ls.close();
        }
        listenSockets.clear();
        
        Logger.info("Closing User Sockets");
        UserSocket.closeAll("BNC Shutdown");
        
        Logger.info("Saving Accounts");
        AccountManager.shutdown();
        AccountManager.saveAccounts();
        
        Logger.info("Saving config to '"+configFile+"'");
        config.save();
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
     *
     * @return the CLIParser.
     */
    private static void setupCLIParser() {
        cli.clear();
        cli.add(new BooleanParam('h', "help", "Show Help"));
        cli.add(new BooleanParam('d', "debug", "Enable extra debugging. (Use twice for more)"));
        cli.add(new BooleanParam('s', "silent", "Disable all output"));
        cli.add(new BooleanParam((char)0, "convert", "Convert old (delphi) style config file to new style"));
        cli.add(new StringParam('c', "config", "Alternative config file to use"));
        cli.add(new BooleanParam((char)0, "enableDebugOptions", "Enable debugging. config settings"));
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
     * Get the default settings.
     *
     * @return Defaults config settings
     *
     * @throws IOException If an error occurred loading the config
     * @throws InvalidConfigFileException If the config was invalid
     */
    public static Config createDefaultConfig() throws IOException,
            InvalidConfigFileException {
        final File directory = new File(configDirectory);
        final File file = new File(directory, configFile);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        final Config defaults = new Config(file);
        if (!defaults.hasOption("general", "bindhost")) {
            defaults.setOption("general", "bindhost", "0.0.0.0");
        }
        if (!defaults.hasOption("general", "bindport")) {
            defaults.setOption("general", "bindport", "33262");
        }
        if (!defaults.hasOption("general", "serverName")) {
            defaults.setOption("general", "serverName", "DFBnc.Server");
        }

        return defaults;
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
     * Start the application.
     *
     * @param args CLI Arguments passed to application
     */
    public static void main(String[] args) { me = new DFBnc(args); }
}

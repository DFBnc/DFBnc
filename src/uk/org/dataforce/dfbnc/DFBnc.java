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
import uk.org.dataforce.logger.LogLevel;
import uk.org.dataforce.cliparser.CLIParser;
import uk.org.dataforce.cliparser.CLIParam;
import uk.org.dataforce.cliparser.BooleanParam;
import uk.org.dataforce.cliparser.StringParam;
import uk.org.dataforce.cliparser.IntegerParam;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.servers.ServerManager;

import java.io.IOException;
import java.util.ArrayList;

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
	private static String configFile = "DFBnc.conf";
	
	/** The command manager for this bnc */
	private static CommandManager myCommandManager = new CommandManager();
	
	/** The server manager for this bnc */
	private static ServerManager myServerManager = new ServerManager();
	
	/** The arraylist of listenSockets */
	private static ArrayList<ListenSocket> listenSockets = new ArrayList<ListenSocket>();

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
		
		if (cli.getParamNumber("-config") > 0) { configFile = cli.getParam("-type").getStringValue(); }
		Logger.info("Loading Config..");
		Config.loadConfig(configFile);
		
		Logger.info("Setting up Command Manager");
		myCommandManager.init();
		
		Logger.info("Setting up Server Manager");
		myServerManager.init();
		
		Logger.info("Loading Accounts..");
		Account.loadAccounts();
		
		Logger.info("Adding shutdown hook");
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
		
		try {
			// This allows for adding multiple listen sockets, altho it is not currently implemented
			Logger.info("Opening Socket..");
			listenSockets.add(new ListenSocket(Config.getOption("general", "bindhost", "0.0.0.0"), Config.getIntOption("general", "bindport", 33262)));
		} catch (IOException e) {
			Logger.error("Unable to open socket: "+e.getMessage());
			Logger.info("Terminating");
			System.exit(0);
		}
		Logger.info("Running!");
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
		Account.saveAccounts();
		
		Logger.info("Saving config to '"+configFile+"'");
		Config.saveConfig(configFile);
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
	 * Get the CommandManager
	 *
	 * @return The CommandManager
	 */
	public static CommandManager getCommandManager() {
		return myCommandManager;
	}
	
	/**
	 * Get the ServerManager
	 *
	 * @return The ServerManager
	 */
	public static ServerManager getServerManager() {
		return myServerManager;
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
	 * Start the application.
	 *
	 * @param args CLI Arguments passed to application
	 */
	public static void main(String[] args) { me = new DFBnc(args); }
}
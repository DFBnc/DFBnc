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
// import uk.org.dataforce.dfbnc.commands.admin.*;
import uk.org.dataforce.dfbnc.commands.user.*;
import uk.org.dataforce.dfbnc.servers.ServerTypeManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	/** The user command manager for this bnc */
	private static CommandManager userCommandManager = new CommandManager();
	
	/** The admin command manager for this bnc */
	private static CommandManager adminCommandManager = new CommandManager();
	
	/** The ServerType manager for this bnc */
	private static ServerTypeManager myServerTypeManager = new ServerTypeManager();
	
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
		
		Logger.info("Setting up User Command Manager");
		userCommandManager.addCommand(new VersionCommand(userCommandManager));
		userCommandManager.addCommand(new FirstTimeCommand(userCommandManager));
		userCommandManager.addCommand(new ServerTypeCommand(userCommandManager));
		userCommandManager.addCommand(new ShowCommandsCommand(userCommandManager));
		
		Logger.info("Setting up Admin Command Manager");
		
		Logger.info("Setting up ServerType Manager");
		myServerTypeManager.init();
		
		Logger.info("Loading Accounts..");
		Account.loadAccounts();
		
		Logger.info("Adding shutdown hook");
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
		
		Logger.info("Opening Listen Sockets..");
		int count = 0;
		List<String> defaulthosts = new ArrayList<String>();
		defaulthosts.add("0.0.0.0:33262");
		List<String> listenhosts = Config.getProperties().getListProperty("general.listenhost", defaulthosts);
		
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
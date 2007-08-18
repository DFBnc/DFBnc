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

import java.io.IOException;

/**
 * Main BNC Class.
 */
public class DFBnc {
	/** Version String */
	public final static String VERSION = "DFBnc-Java 0.1";
	
	/** The CLIParser */
	private CLIParser cli = CLIParser.getCLIParser();

	/**
	 * Run the application.
	 * Parses CLI Arguments, loads config file, and sets up the listen sockets.
	 *
	 * @param args CLI Arguments passed to application
	 */
	public DFBnc(String[] args) {
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
		
		String configFile = "DFBnc.conf";
		if (cli.getParamNumber("-config") > 0) { configFile = cli.getParam("-type").getStringValue(); }
		Logger.info("Loading Config..");
		Config.loadConfig(configFile);
		
		try {
			Logger.info("Opening Socket..");
			ListenSocket listenSocet = new ListenSocket(Config.getOption("general", "bindhost", "0.0.0.0"), Config.getIntOption("general", "bindport", 33262));
		} catch (IOException e) {
			Logger.error("Unable to open socket: "+e.getMessage());
			Logger.info("Terminating");
			System.exit(0);
		}
		Logger.info("Running!");
	}
	
	/**
	 * Setup the cli parser.
	 * This clears the current CLIParser params and creates new ones.
	 *
	 * @return the CLIParser.
	 */
	private void setupCLIParser() {
		cli.clear();
		cli.add(new BooleanParam('h', "help", "Show Help"));
		cli.add(new BooleanParam('d', "debug", "Enable extra debugging. (Use twice for more)"));
		cli.add(new BooleanParam('s', "silent", "Disable all output"));
		cli.add(new BooleanParam((char)0, "convert", "Convert old (delphi) style config file to new style"));
		cli.add(new StringParam('c', "config", "Alternative config file to use"));
		cli.setHelp(cli.getParam("-help"));
	}

	/**
	 * Start the application.
	 *
	 * @param args CLI Arguments passed to application
	 */
	public static void main(String[] args) { new DFBnc(args); }
}
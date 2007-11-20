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

import uk.org.dataforce.libs.logger.Logger;
import uk.org.dataforce.libs.util.TypedProperties;
import uk.org.dataforce.libs.cliparser.CLIParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Configuration Files.
 */
public class Config {
	/** Config File */
	private static TypedProperties config = getDefaults();
	
	/**
	 * Get the properties object.
	 *
	 * @return The properties object
	 */
	public static TypedProperties getProperties() {
		return config;
	}
	
	/**
	 * Get the default settings.
	 *
	 * @return Defaults config settings
	 */
	private static TypedProperties getDefaults() {
		final TypedProperties defaults = new TypedProperties();
		defaults.setCaseSensitivity(false);
		defaults.setProperty("General.bindhost", "0.0.0.0");
		defaults.setIntProperty("General.bindport", 33262);
		defaults.setProperty("General.serverName", "DFBnc.Server");
		defaults.setIntProperty("users.count", 0);
		return defaults;
	}
	
	/**
	 * Load a new config file
	 *
	 * @param filename Filename of config to load
	 */
	public static void loadConfig(final String filename) {
		config = new TypedProperties(getDefaults());
		config.setCaseSensitivity(false);
		final File file = new File(filename);
		if (file.exists()) {
			try {
				config.load(new FileInputStream(file));
			} catch (IOException e) {
				Logger.error("Unable to load config from '"+filename+"': "+e);
			}
		}
	}
	
	/**
	 * Save the accounts to the config file, and then the config file to disk.
	 *
	 * @param filename Filename to save config to
	 */
	public static void saveAll(final String filename) {
		Account.saveAccounts();
		Logger.info("Saving config to '"+filename+"'");
		saveConfig(filename);
	}
	
	/**
	 * Save the current config file to disk
	 *
	 * @param filename Filename to save config to
	 */
	public static void saveConfig(final String filename) {
		final File file = new File(filename);
		try {
			config.store(new FileOutputStream(file), "DFBNC Config File");
		} catch (IOException e) {
			Logger.error("Unable to save config to '"+filename+"': "+e);
		}
	}
	
	/**
	 * Check if debugging options are disabled.
	 *
	 * @return true if debugging options are disabled (and the given domain is debugging)
	 *         this makes get* options return the fallback when a debugging option is requested
	 */
	private static boolean checkDebugging(final String domain) {
		return (domain.equalsIgnoreCase("debugging") && CLIParser.getCLIParser().getParamNumber("-enableDebugOptions") < 1);
	}
	
	/**
	 * Get an option from the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @param fallback Value to return if key is not found
	 * @return the requested option, or the fallback value if not defined
	 */
	public static String getOption(final String domain, final String key, final String fallback) {
		if (checkDebugging(domain)) {
			return fallback;
		} else {
			return config.getProperty(domain.toLowerCase()+"."+key.toLowerCase(), fallback);
		}
	}
	
	/**
	 * Set an option in the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @param value Value for option
	 */
	public static void setOption(final String domain, final String key, final String value) {
		config.setProperty(domain.toLowerCase()+"."+key.toLowerCase(), value);
	}
	
	/**
	 * Check if an option exists in the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @return True if the option exists, else false
	 */
	public static boolean hasOption(final String domain, final String key) {
		if (checkDebugging(domain)) {
			return false;
		} else {
			return config.hasProperty(domain.toLowerCase()+"."+key.toLowerCase());
		}
	}
	
	/**
	 * Get a boolean option from the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @param fallback Value to return if key is not found
 	 * @return the requested option, or the fallback value if not defined
	 */
	public static boolean getBoolOption(final String domain, final String key, final boolean fallback) {
		if (checkDebugging(domain)) {
			return fallback;
		} else {
			return config.getBoolProperty(domain.toLowerCase()+"."+key.toLowerCase(), fallback);
		}
	}
	
	/**
	 * Set a Boolean option in the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @param value Value for option
	 */
	public static void setBoolOption(final String domain, final String key, final boolean value) {
		config.setBoolProperty(domain.toLowerCase()+"."+key.toLowerCase(), value);
	}
	
	/**
	 * Get an integer option from the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @param fallback Value to return if key is not found
	 * @return the requested option, or the fallback value if not defined
	 */
	public static int getIntOption(final String domain, final String key, final int fallback) {
		if (checkDebugging(domain)) {
			return fallback;
		} else {
			return config.getIntProperty(domain.toLowerCase()+"."+key.toLowerCase(), fallback);
		}
	}
	
	/**
	 * Set an integer option in the config
	 *
	 * @param domain Domain for option
	 * @param key key for option
	 * @param value Value for option
	 */
	public static void setIntOption(final String domain, final String key, final int value) {
		config.setIntProperty(domain.toLowerCase()+"."+key.toLowerCase(), value);
	}

	/**
	 * Prevent Creation of Config Object
	 */
	private Config() { }
}
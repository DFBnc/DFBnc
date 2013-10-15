/*
 * Copyright (c) 2006-2013 Shane Mc Cormack, Gregory Holmes
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

package uk.org.dataforce.dfbnc.config;

import com.dmdirc.util.collections.WeakList;
import com.dmdirc.util.io.ConfigFile;
import com.dmdirc.util.io.InvalidConfigFileException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import uk.org.dataforce.libs.logger.Logger;

/**
 * Configuration Files.
 */
public class Config {

    /** Config File */
    private final ConfigFile config;

    /** Config Changed Listeners. */
    private final List<ConfigChangedListener> changeListeners = new WeakList<ConfigChangedListener>();

    /**
     * Creates a new config based on the specified file.
     *
     * @param filename Path to file to create config based on
     *
     * @throws IOException IO Error reading config file
     * @throws InvalidConfigFileException Invalid config file
     */
    public Config(final String filename) throws IOException, InvalidConfigFileException {
        this(new File(filename));
    }

    /**
     * Creates a new config based on the specified file.
     *
     * @param file File to create config based on
     *
     * @throws IOException IO Error reading config file
     * @throws InvalidConfigFileException Invalid config file
     */
    public Config(final File file) throws IOException, InvalidConfigFileException {
        this(new ConfigFile(file));
    }

    /**
     * Creates a new config based on the specified input stream.
     *
     * @param is Input stream to create config based on
     *
     * @throws IOException IO Error reading config file
     * @throws InvalidConfigFileException Invalid config file
     */
    public Config(final InputStream is) throws IOException, InvalidConfigFileException {
        this(new ConfigFile(is));
    }

    /**
     * Creates a new config based on the specified config file.
     *
     * @param configFile Config file to create config based on
     *
     * @throws IOException IO Error reading config file
     * @throws InvalidConfigFileException Invalid config file
     */
    public Config(final ConfigFile configFile) throws IOException, InvalidConfigFileException {
        config = configFile;
        init();
    }

    /**
     * Initialises this config.
     *
     * @throws FileNotFoundException Config file not found
     * @throws IOException IO Error reading config file
     * @throws InvalidConfigFileException Invalid config file
     */
    private void init() throws IOException, InvalidConfigFileException {
        if (config.getFile() != null && !config.getFile().exists()) {
            if (!config.getFile().createNewFile()) {
                throw new IOException("Unable to create config file.");
            }
        }
        config.read();
        config.setAutomake(true);
    }


    /**
     * Handle a setting change - let all listeners know.
     *
     * @param domain Domain that changed
     * @param key Setting that changed
     */
    private void handleSettingChange(final String domain, final String key) {
        for (ConfigChangedListener listener : new ArrayList<ConfigChangedListener>(changeListeners)) {
            listener.configChanged(domain, key);
        }
    }


    /**
     * Adds a new ConfigChangedListener for this config.
     *
     * @param listener The listener to be added
     */
    public void addListener(final ConfigChangedListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes the given ConfigChangedListener from this config.
     *
     * @param listener The listener to be removed
     */
    public void removeListener(final ConfigChangedListener listener) {
        changeListeners.remove(listener);
    }

    /**
     * Get option domain from the config
     *
     * @param domain Domain for option
     * @return the requested option domain, or null
     */
    public Map<String, String> getOptionDomain(final String domain) {
        return config.getKeyDomain(domain);
    }

    /**
     * Get an option from the config
     *
     * @param domain Domain for option
     * @param key key for option
     * @param fallback Value to return if key is not found
     * @return the requested option, or the fallback value if not defined
     */
    public String getOption(final String domain, final String key, final String fallback) {
        final String value = config.getKeyDomain(domain).get(key);
        if (value == null) {
            return fallback;
        } else {
            return value;
        }
    }

    /**
     * Set an option in the config
     *
     * @param domain Domain for option
     * @param key key for option
     * @param value Value for option
     */
    public void setOption(final String domain, final String key, final String value) {
        config.getKeyDomain(domain).put(key, value);
        handleSettingChange(domain, key);
    }

    /**
     * Check if an option exists in the config
     *
     * @param domain Domain for option
     * @param key key for option
     * @return True if the option exists, else false
     */
    public boolean hasOption(final String domain, final String key) {
        return config.hasDomain(domain) != false && config.getKeyDomain(domain).containsKey(key);
    }

    /**
     * Get a Byte Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public byte getByteOption(final String domain, final String key, final byte fallback) {
        try {
            return Byte.parseByte(getOption(domain, key, Byte.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Set a Byte Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setByteOption(final String domain, final String key, final byte value) {
        setOption(domain, key, Byte.toString(value));
    }

    /**
     * Get a Short Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public short getShortOption(final String domain, final String key, final short fallback) {
        try {
            return Short.parseShort(getOption(domain, key, Short.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Set a Short Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setShortOption(final String domain, final String key, final short value) {
        setOption(domain, key, Short.toString(value));
    }

    /**
     * Get an integer Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public int getIntOption(final String domain, final String key, final int fallback) {
        try {
            return Integer.parseInt(getOption(domain, key, Integer.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Set an integer Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setIntOption(final String domain, final String key, final int value) {
        setOption(domain, key, Integer.toString(value));
    }

    /**
     * Get a Long Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public long getLongOption(final String domain, final String key, final long fallback) {
        try {
            return Long.parseLong(getOption(domain, key, Long.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Set a Long Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setLongOption(final String domain, final String key, final long value) {
        setOption(domain, key, Long.toString(value));
    }

    /**
     * Get a float Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public float getFloatOption(final String domain, final String key, final float fallback) {
        try {
            return Float.parseFloat(getOption(domain, key, Float.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Set a float Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setFloatOption(final String domain, final String key, final float value) {
        setOption(domain, key, Float.toString(value));
    }

    /**
     * Get a double Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public double getDoubleOption(final String domain, final String key, final double fallback) {
        try {
            return Double.parseDouble(getOption(domain, key, Double.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    /**
     * Set a double Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setDoubleOption(final String domain, final String key, final double value) {
        setOption(domain, key, Double.toString(value));
    }

    /**
     * Get a boolean Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public boolean getBoolOption(final String domain, final String key, final boolean fallback) {
        return Boolean.parseBoolean(getOption(domain, key, Boolean.toString(fallback)));
    }

    /**
     * Set a Boolean Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setBoolOption(final String domain, final String key, final boolean value) {
        setOption(domain, key, Boolean.toString(value));
    }

    /**
     * Get a Char Option from the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback Value to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public char getCharOption(final String domain, final String key, final char fallback) {
        final String res = getOption(domain, key, Character.toString(fallback));
        if (res == null || res.isEmpty()) {
            return fallback;
        } else {
            return res.charAt(0);
        }
    }

    /**
     * Set a Char Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setCharOption(final String domain, final String key, final char value) {
        setOption(domain, key, Character.toString(value));
    }

    /**
     * Get a List Option from the config.
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param fallback List to return if key is not found
     * @return the requested Option, or the fallback value if not defined
     */
    public List<String> getListOption(final String domain, final String key, final List<String> fallback) {
        final String res = getOption(domain, key, "");
        if (res == null || res.isEmpty()) {
            return fallback;
        } else {
            final String bits[] = res.split("\n");
            final ArrayList<String> result = new ArrayList<String>();
            for (String bit : bits) {
                result.add(bit);
            }
            return result;
        }
    }

    /**
     * Set a List Option in the config
     *
     * @param domain Domain for option
     * @param key key for Option
     * @param value Value for Option
     */
    public void setListOption(final String domain, final String key, final List<String> value) {
        final StringBuilder val = new StringBuilder();
        final String LF = "\n";
        boolean first = true;
        for (String bit : value) {
            if (first) {
                first = false;
            } else {
                val.append(LF);
            }
            val.append(bit);
        }
        setOption(domain, key, val.toString());
    }

    /**
     * Saves this config to disk.
     */
    public void save() {
        try {
            config.write();
        } catch (IOException ex) {
            Logger.error("Unable to save config: " + ex.getMessage());
        }
    }
}

/*
 * Copyright (c) 2006-2012 DMDirc Developers
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

package com.dfbnc.config;

import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Config {

    /**
     * Retrieves the first value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public String getOption(final String domain, final String option);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(final String domain, final String option, final String value);

    /**
     * Unsets the specified option in this configuration.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     */
    public void unsetOption(final String domain, final String option);

    /**
     * Retrieves the first value for the specified option that matches the
     * specified validator.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param validator The validator to use to check legal values
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public String getOption(final String domain, final String option, Validator<String> validator);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     * @param validator The validator to verify the setting against
     */
    public void setOption(final String domain, final String option, final String value, Validator<String> validator);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(final String domain, final String option, final boolean value);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(final String domain, final String option, final int value);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(final String domain, final String option, final float value);

    /**
     * Retrieves the first value for the specified option and returns it as a
     * boolean.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return The value of the option, or false if no matching values exist
     */
    public Boolean getOptionBool(final String domain, final String option);

    /**
     * Retrieves the first value for the specified option that matches the
     * specified validator.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param validator The validator to use to check legal values
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public List<String> getOptionList(final String domain, final String option, final Validator<String> validator);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(final String domain, final String option, final List<String> value);

    /**
     * Retrieves the first value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public List<String> getOptionList(final String domain, final String option);

    /**
     * Retrieves the first value for the specified option that matches the
     * specified validator.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param validator The validator to use to check legal values
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public Integer getOptionInt(final String domain, final String option, final Validator<String> validator);

    /**
     * Retrieves the first value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public Integer getOptionInt(final String domain, final String option);

    /**
     * Determines if this source has a value for the specified option which
     * matches the specified validator.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param validator The validator to use to check legal values
     * <p/>
     * @return True iff a matching option exists, false otherwise.
     */
    public boolean hasOption(final String domain, final String option, final Validator<String> validator);

    /**
     * Determines if this source has a value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return True iff a matching option exists, false otherwise.
     */
    public boolean hasOption(final String domain, final String option);

    /**
     * Returns the name of all the options in the specified domain. If the
     * domain doesn't exist, an empty list is returned.
     *
     * @param domain The domain to search
     * <p/>
     * @return A list of options in the specified domain
     */
    public Map<String, String> getOptions(final String domain);

    /**
     * Returns the name of all domains known by this manager.
     *
     * @return A list of domains known to this manager
     */
    public Set<String> getDomains();

    /**
     * Adds a change listener for the specified domain.
     *
     * @param domain The domain to be monitored
     * @param listener The listener to register
     */
    public void addChangeListener(final String domain, final ConfigChangeListener listener);

    /**
     * Adds a change listener for the specified domain.
     *
     * @param listener The listener to register
     */
    public void addChangeListener(final ConfigChangeListener listener);

    /**
     * Adds a change listener for the specified domain and key.
     *
     * @param domain The domain of the option
     * @param key The option to be monitored
     * @param listener The listener to register
     */
    public void addChangeListener(final String domain, final String key, final ConfigChangeListener listener);

    /**
     * Removes the specified listener for all domains and options.
     *
     * @param listener The listener to be removed
     */
    public void removeListener(final ConfigChangeListener listener);

    /**
     * Saves this configuration to disk.
     */
    public void save();

    /**
     * Initialises the configuration file.
     * <p/>
     * @throws IOException If an error occurs reading the configuration file
     * @throws InvalidConfigFileException If the configuration file is invalid
     * or corrupt
     */
    public void init() throws IOException, InvalidConfigFileException;
}

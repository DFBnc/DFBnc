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

package uk.org.dataforce.dfbnc.config;

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
    public String getOption(String domain, String option);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(String domain, String option, final String value);

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
    public String getOption(String domain, String option,
            Validator<String> validator);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     * @param validator The validator to verify the setting against
     */
    public void setOption(String domain, String option, final String value,
            Validator<String> validator);

    public void setOption(String domain, String option, boolean value);

    public void setOption(String domain, String option, int value);

    public void setOption(String domain, String option, float value);

    public Boolean getOptionBool(String domain, String option);

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
    public List<String> getOptionList(String domain, String option, Validator<String> validator);

    /**
     * Sets the specified option in this configuration to the specified value.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * @param value The new value for the option
     */
    public void setOption(String domain, String option, List<String> value);

    /**
     * Retrieves the first value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public List<String> getOptionList(String domain, String option);

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
    public Integer getOptionInt(String domain, String option, Validator<String> validator);

    /**
     * Retrieves the first value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return The value of the option, or null if no matching values exist
     */
    public Integer getOptionInt(String domain, String option);

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
    public boolean hasOption(String domain, String option,
            Validator<String> validator);

    /**
     * Determines if this source has a value for the specified option.
     *
     * @param domain The domain of the option
     * @param option The name of the option
     * <p/>
     * @return True iff a matching option exists, false otherwise.
     */
    public boolean hasOption(String domain, String option);

    /**
     * Returns the name of all the options in the specified domain. If the
     * domain doesn't exist, an empty list is returned.
     *
     * @param domain The domain to search
     * <p/>
     * @return A list of options in the specified domain
     */
    public Map<String, String> getOptions(String domain);

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
    public void addChangeListener(String domain,
            ConfigChangeListener listener);

    /**
     * Adds a change listener for the specified domain.
     *
     * @param listener The listener to register
     */
    public void addChangeListener(ConfigChangeListener listener);

    /**
     * Adds a change listener for the specified domain and key.
     *
     * @param domain The domain of the option
     * @param key The option to be monitored
     * @param listener The listener to register
     */
    public void addChangeListener(String domain, String key,
            ConfigChangeListener listener);

    /**
     * Removes the specified listener for all domains and options.
     *
     * @param listener The listener to be removed
     */
    public void removeListener(ConfigChangeListener listener);

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

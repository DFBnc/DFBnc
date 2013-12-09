/*
 * Copyright 2012 Greg 'Greboid' Holmes.
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package uk.org.dataforce.dfbnc.config;

import com.dmdirc.util.collections.MapList;
import com.dmdirc.util.io.ConfigFile;
import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.PermissiveValidator;
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single layer configuration
 */
public class DefaultsConfig implements Config {

    /**
     * A validator which succeeds on all values.
     */
    private final Validator<String> permissiveValidator;
    /**
     * User level configuration file, overrides defaults.
     */
    private final ConfigFile config;
    /**
     * System level configuration file used for defaults.
     */
    private final ConfigFile defaults;
    /**
     * Configuration change listeners.
     */
    private final MapList<String, ConfigChangeListener> listeners;

    /**
     * Creates a new configuration file, creating the file is needed.
     */
    public DefaultsConfig(final ConfigFile config, final ConfigFile defaults) throws IOException, InvalidConfigFileException {
        listeners = new MapList<>();
        permissiveValidator = new PermissiveValidator<>();
        this.defaults = defaults;
        this.config = config;

        if (config.getFile() != null && !config.getFile().exists()) {
            if (!config.getFile().createNewFile()) {
                throw new IOException("Unable to create config file.");
            }
        }
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOption(final String domain, final String option, final Validator<String> validator) {
        String value = null;
        try {
            value = config.getKeyDomain(domain).get(option);
        } catch (final Exception e) { /** Option not found in this config. */ }

        if (validator.validate(value).isFailure()) {
            value = null;
        }

        if (value == null) {
            try {
                value = defaults.getKeyDomain(domain).get(option);
            } catch (final Exception e) { /** Option not found in this config either. */ }
        }

        if (value == null) {
            throw new NullPointerException("No such config option: " + domain + "." + option);
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOption(final String domain, final String option, final String value) {
        config.getKeyDomain(domain).put(option, value);
        if (listeners.containsKey(domain)) {
            for (ConfigChangeListener listener : listeners.get(domain)) {
                listener.configChanged(domain, option);
            }
        }
        if (listeners.containsKey(domain + "." + option)) {
            for (ConfigChangeListener listener : listeners.get(domain + "." + option)) {
                listener.configChanged(domain, option);
            }
        }
        if (listeners.containsKey("")) {
            for (ConfigChangeListener listener : listeners.get("")) {
                listener.configChanged(domain, option);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOption(final String domain, final String option, final String value, final Validator<String> validator) {
        if (!validator.validate(value).isFailure()) {
            setOption(domain, option, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOption(final String domain, final String option, final boolean value) {
        setOption(domain, option, Boolean.toString(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOption(final String domain, final String option, final int value) {
        setOption(domain, option, Integer.toString(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOption(final String domain, final String option, final float value) {
        setOption(domain, option, Float.toString(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getOptionBool(final String domain, final String option) {
        return Boolean.valueOf(getOption(domain, option));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getOptionList(final String domain, final String option) {
        return getOptionList(domain, option, permissiveValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getOptionList(final String domain, final String option, final Validator<String> validator) {
        final List<String> res = new ArrayList<>();

        for (String line : getOption(domain, option, validator).split("\n")) {
            if (!line.isEmpty()) {
                res.add(line);
            }
        }

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOption(final String domain, final String option, final List<String> value) {
        final StringBuilder temp = new StringBuilder();
        for (String part : value) {
            temp.append('\n');
            temp.append(part);
        }
        setOption(domain, option, temp.length() > 0 ? temp.substring(1) : temp.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getOptionInt(final String domain, final String option, final Validator<String> validator) {
        final String value = getOption(domain, option, validator);
        Integer intVal;
        try {
            intVal = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            intVal = null;
        }
        return intVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getOptionInt(final String domain, final String option) {
        return getOptionInt(domain, option, permissiveValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOption(final String domain, final String option) {
        return getOption(domain, option, permissiveValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOption(final String domain, final String option, final Validator<String> validator) {
        String value = config.getKeyDomain(domain).get(option);
        return value != null && !validator.validate(value).isFailure();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOption(final String domain, final String option) {
        return hasOption(domain, option, permissiveValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getOptions(final String domain) {
        return config.getKeyDomain(domain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getDomains() {
        return config.getKeyDomains().keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChangeListener(final String domain, final ConfigChangeListener listener) {
        listeners.add(domain, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChangeListener(final ConfigChangeListener listener) {
        listeners.add("", listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChangeListener(final String domain, final String key, final ConfigChangeListener listener) {
        listeners.add(domain + "." + key, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(final ConfigChangeListener listener) {
        listeners.removeFromAll(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        try {
            config.write();
        } catch (IOException ex) {
            //Oh shit.
        }
    }

    /**
     * Initialises the config files.
     * <p/>
     * @throws IOException If the config files do not exist
     * @throws InvalidConfigFileException If the config files are invalid
     */
    @Override
    public void init() throws IOException, InvalidConfigFileException {
        defaults.read();
        defaults.setAutomake(true);
        config.read();
        config.setAutomake(true);
    }
}

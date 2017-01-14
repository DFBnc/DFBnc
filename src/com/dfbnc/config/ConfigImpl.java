/*
 * Copyright (c) 2006-2017 DFBnc Developers
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

package com.dfbnc.config;

import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.PermissiveValidator;
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single layer configuration
 */
public abstract class ConfigImpl implements Config {

    /**
     * A validator which succeeds on all values.
     */
    private final Validator<String> permissiveValidator = new PermissiveValidator<>();
    /**
     * Configuration change listeners.
     */
    private final Map<String, List<ConfigChangeListener>> listeners = new HashMap<>();

    /**
     * Creates a new configuration file, creating the file is needed.
     */
    public ConfigImpl() throws IOException, InvalidConfigFileException {
    }

    @Override
    public void setOption(final String domain, final String option, final String value, final Validator<String> validator) {
        if (!validator.validate(value).isFailure()) {
            setOption(domain, option, value);
        }
    }

    @Override
    public void setOption(final String domain, final String option, final boolean value) {
        setOption(domain, option, Boolean.toString(value));
    }

    @Override
    public void setOption(final String domain, final String option, final int value) {
        setOption(domain, option, Integer.toString(value));
    }

    @Override
    public void setOption(final String domain, final String option, final float value) {
        setOption(domain, option, Float.toString(value));
    }

    @Override
    public Boolean getOptionBool(final String domain, final String option) {
        return Boolean.valueOf(getOption(domain, option));
    }

    @Override
    public List<String> getOptionList(final String domain, final String option) {
        return getOptionList(domain, option, permissiveValidator);
    }

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

    @Override
    public void setOption(final String domain, final String option, final List<String> value) {
        final StringBuilder temp = new StringBuilder();
        for (String part : value) {
            temp.append('\n');
            temp.append(part);
        }
        setOption(domain, option, temp.length() > 0 ? temp.substring(1) : temp.toString());
    }

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

    @Override
    public Integer getOptionInt(final String domain, final String option) {
        return getOptionInt(domain, option, permissiveValidator);
    }

    @Override
    public String getOption(final String domain, final String option) {
        return getOption(domain, option, permissiveValidator);
    }

    @Override
    public boolean hasOption(final String domain, final String option) {
        return hasOption(domain, option, permissiveValidator);
    }

    @Override
    public void addChangeListener(final String domain, final ConfigChangeListener listener) {
        addListener(domain, listener);
    }

    @Override
    public void addChangeListener(final ConfigChangeListener listener) {
        addListener("", listener);
    }

    @Override
    public void addChangeListener(final String domain, final String key, final ConfigChangeListener listener) {
        addListener(domain + "." + key, listener);
    }

    /**
     * Adds a change listener to the list of listeners.
     *
     * @param key the key to listen for.
     * @param listener The listener to register
     */
    protected void addListener(final String key, final ConfigChangeListener listener) {
        if (!listeners.containsKey(key)) {
            final List<ConfigChangeListener> l = new ArrayList<>();
            listeners.put(key, l);
        }
        if (!listeners.get(key).contains(listener)) {
            listeners.get(key).add(listener);
        }
    }

    @Override
    public void removeListener(final ConfigChangeListener listener) {
        listeners.values().stream().forEach((list) -> list.remove(listener));
    }

    /**
     * Call matching listeners
     *
     * @param domain the domain that changed
     * @param option the option that changed
     */
    protected void callListeners(final String domain, final String option) {
        if (listeners.containsKey(domain)) {
            for (final ConfigChangeListener listener : listeners.get(domain)) {
                listener.configChanged(this, domain, option);
            }
        }
        if (listeners.containsKey(domain + "." + option)) {
            for (final ConfigChangeListener listener : listeners.get(domain + "." + option)) {
                listener.configChanged(this, domain, option);
            }
        }
        if (listeners.containsKey("")) {
            for (final ConfigChangeListener listener : listeners.get("")) {
                listener.configChanged(this, domain, option);
            }
        }
    }
}

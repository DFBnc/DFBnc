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
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Single layer configuration
 */
public class DefaultsConfig extends ConfigImpl {

    /**
     * User level configuration file, overrides defaults.
     */
    private final Config config;
    /**
     * System level configuration file used for defaults.
     */
    private final Config defaults;

    /**
     * Creates a new configuration file, creating the file is needed.
     */
    public DefaultsConfig(final Config config, final Config defaults) throws IOException, InvalidConfigFileException {
        super();
        this.defaults = defaults;
        this.config = config;

        if (defaults == null || config == null) {
            throw new InvalidConfigFileException("Null Config Given");
        }

        init();
    }

    @Override
    public String getOption(final String domain, final String option, final Validator<String> validator) {
        String value = null;
        try {
            value = config.getOption(domain, option, validator);
        } catch (final NullPointerException npe) { }

        if (value == null) {
            try {
                value = defaults.getOption(domain, option, validator);
            } catch (final NullPointerException npe) { }
        }

        if (value == null) {
            throw new NullPointerException("No such config option: " + domain + "." + option);
        }

        return value;
    }

    @Override
    public void setOption(final String domain, final String option, final String value) {
        config.setOption(domain, option, value);

        callListeners(domain, option);
    }

    @Override
    public void unsetOption(final String domain, final String option) {
        config.unsetOption(domain, option);

        callListeners(domain, option);
    }

    @Override
    public boolean hasOption(final String domain, final String option, final Validator<String> validator) {
        return config.hasOption(domain, option, validator) || defaults.hasOption(domain, option, validator);
    }

     @Override
    public Map<String, String> getOptions(final String domain) {
        return config.getOptions(domain);
    }

    @Override
    public Set<String> getDomains() {
        return config.getDomains();
    }

    @Override
    public void save() {
        config.save();
    }

    /**
     * Does nothing, ConfigFileConfig handles this.
     */
    @Override
    public void init() throws IOException, InvalidConfigFileException { }
}

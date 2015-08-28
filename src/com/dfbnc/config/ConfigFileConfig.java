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

package com.dfbnc.config;

import com.dmdirc.util.io.ConfigFile;
import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Single layer configuration
 */
public class ConfigFileConfig extends ConfigImpl {

    /**
     * User level configuration file, overrides defaults.
     */
    private final ConfigFile config;

    /**
     * Creates a new configuration file, creating the file is needed.
     *
     * @param file File to create ConfigFile from
     */
    public ConfigFileConfig(final File file) throws IOException, InvalidConfigFileException {
        super();

        if (file == null) {
            throw new IOException("Unable to create config file.");
        }
        if (file != null && !file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Unable to create config file.");
            }
         }

        this.config = new ConfigFile(file.toPath());

        init();
    }

    /**
     * Creates a new configuration file from an input stream.
     *
     * @param stream InputStream to create ConfigFile from
     */
    public ConfigFileConfig(final InputStream stream) throws IOException, InvalidConfigFileException {
        super();
        this.config = new ConfigFile(stream);

        init();
    }

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
            throw new NullPointerException("No such config option: " + domain + "." + option);
        }

        return value;
    }

    @Override
    public void setOption(final String domain, final String option, final String value) {
        config.getKeyDomain(domain).put(option, value);

        callListeners(domain, option);
    }

    @Override
    public void unsetOption(final String domain, final String option) {
        config.getKeyDomain(domain).remove(option);

        callListeners(domain, option);
    }

    @Override
    public boolean hasOption(final String domain, final String option, final Validator<String> validator) {
        String value = config.hasDomain(domain) ? config.getKeyDomain(domain).get(option) : null;

        return value != null && !validator.validate(value).isFailure();
    }

     @Override
    public Map<String, String> getOptions(final String domain) {
        return config.getKeyDomain(domain);
    }

    @Override
    public Set<String> getDomains() {
        return config.getKeyDomains().keySet();
    }

    @Override
    public void save() {
        try {
            config.write();
        } catch (IOException ex) {
            //Oh shit.
        }
    }

    /**
     * Initialises the config file.
     * <p/>
     * @throws IOException If the config file do not exist
     * @throws InvalidConfigFileException If the config file is invalid
     */
    @Override
    public void init() throws IOException, InvalidConfigFileException {
        config.read();
        config.setAutomake(true);
    }
}

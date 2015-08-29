/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.dfbnc.config;

import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around an existing Config to make it ReadOnly
 */
public class ReadOnlyConfig implements Config {

    private final Config config;

    public ReadOnlyConfig(final Config config) throws IOException, InvalidConfigFileException {
        this.config = config;
    }

    @Override
    public String getOption(final String domain, final String option) {
        return config.getOption(domain, option);
    }

    @Override
    public void setOption(final String domain, final String option, final String value) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void unsetOption(final String domain, final String option) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public String getOption(final String domain, final String option, final Validator<String> validator) {
        return config.getOption(domain, option, validator);
    }

    @Override
    public void setOption(final String domain, final String option, final String value, final Validator<String> validator) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public Boolean getOptionBool(final String domain, final String option) {
        return config.getOptionBool(domain, option);
    }

    @Override
    public List<String> getOptionList(final String domain, final String option, final Validator<String> validator) {
        return config.getOptionList(domain, option, validator);
    }

    @Override
    public void setOption(final String domain, final String option, final List<String> value) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public List<String> getOptionList(final String domain, final String option) {
        return config.getOptionList(domain, option);
    }

    @Override
    public Integer getOptionInt(final String domain, final String option, final Validator<String> validator) {
        return config.getOptionInt(domain, option, validator);
    }

    @Override
    public Integer getOptionInt(final String domain, final String option) {
        return config.getOptionInt(domain, option);
    }

    @Override
    public boolean hasOption(final String domain, final String option, final Validator<String> validator) {
        return config.hasOption(domain, option, validator);
    }

    @Override
    public boolean hasOption(final String domain, final String option) {
        return config.hasOption(domain, option);
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
    public void addChangeListener(final String domain, final ConfigChangeListener listener) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void addChangeListener(final String domain, final String key, final ConfigChangeListener listener) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void removeListener(final ConfigChangeListener listener) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void init() throws IOException, InvalidConfigFileException {
        config.init();
    }

    @Override
    public void setOption(final String domain, final String option, final boolean value) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void setOption(final String domain, final String option, final int value) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void setOption(final String domain, final String option, final float value) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public void addChangeListener(final ConfigChangeListener listener) {
        throw new UnsupportedOperationException("This config is read only.");
    }

}

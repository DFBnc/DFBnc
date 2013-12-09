/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.org.dataforce.dfbnc.config;

import com.dmdirc.util.io.ConfigFile;
import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.PermissiveValidator;
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadOnlyConfig implements Config {

    private final ConfigFile config;
    private final Validator<String> permissiveValidator;

    public ReadOnlyConfig(final ConfigFile config) throws IOException, InvalidConfigFileException {
        this.config = config;
        config.read();
        permissiveValidator = new PermissiveValidator<>();
    }

    @Override
    public String getOption(final String domain, final String option) {
        return getOption(domain, option, permissiveValidator);
    }

    @Override
    public void setOption(final String domain, final String option, final String value) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public String getOption(final String domain, final String option, final Validator<String> validator) {
        String value = config.getKeyDomain(domain).get(option);

        if (validator.validate(value).isFailure()) {
            value = null;
        }
        return value;
    }

    @Override
    public void setOption(final String domain, final String option, final String value, final Validator<String> validator) {
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public Boolean getOptionBool(final String domain, final String option) {
        return Boolean.valueOf(getOption(domain, option));
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
        throw new UnsupportedOperationException("This config is read only.");
    }

    @Override
    public List<String> getOptionList(final String domain, final String option) {
        return getOptionList(domain, option, permissiveValidator);
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
    public boolean hasOption(final String domain, final String option, final Validator<String> validator) {
        String value = config.getKeyDomain(domain).get(option);
        return value != null && !validator.validate(value).isFailure();
    }

    @Override
    public boolean hasOption(final String domain, final String option) {
        return hasOption(domain, option, permissiveValidator);
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
        config.read();
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

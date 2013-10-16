/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.org.dataforce.dfbnc.config;

import com.dmdirc.util.io.ConfigFile;
import com.dmdirc.util.io.InvalidConfigFileException;
import com.dmdirc.util.validators.Validator;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadOnlyConfig implements Config {

    public ReadOnlyConfig(ConfigFile configFile) {

    }

    @Override
    public String getOption(String domain, String option) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String domain, String option, String value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getOption(String domain, String option, Validator<String> validator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String domain, String option, String value, Validator<String> validator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean getOptionBool(String domain, String option) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getOptionList(String domain, String option, Validator<String> validator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String domain, String option, List<String> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getOptionList(String domain, String option) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Integer getOptionInt(String domain, String option, Validator<String> validator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Integer getOptionInt(String domain, String option) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasOption(String domain, String option, Validator<String> validator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasOption(String domain, String option) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, String> getOptions(String domain) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getDomains() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addChangeListener(String domain, ConfigChangeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addChangeListener(String domain, String key, ConfigChangeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeListener(ConfigChangeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init() throws IOException, InvalidConfigFileException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String domain, String option, boolean value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String domain, String option, int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String domain, String option, float value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addChangeListener(ConfigChangeListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

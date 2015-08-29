/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.Command;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class CommandOutputFilterManager {
    /** HashMap used to store the different types of Command known. */
    private static final HashMap<String,CommandOutputFilter> knownFilters = new HashMap<>();

    /**
     * Get an instance of a filter.
     *
     * @param filterName Filter to get instance of.
     * @return Filter or null if filterName is invalid.
     */
    public static CommandOutputFilter getFilter(final String filterName) {
        final List<String> filterType = Command.getParamMatch(filterName, knownFilters.keySet());

        if (filterType.size() == 1) {
            return knownFilters.get(filterType.get(0));
        } else if (!filterType.isEmpty()) {
            return null;
        } else {
            return null;
        }
    }

    /**
     * Get a list of all valid filters
     *
     * @return Valid filters
     */
    public static List<String> getFilters() {
        return new ArrayList<>(knownFilters.keySet());
    }

    /** Populate the knownFilters map. */
    static {
        knownFilters.put("include", new IncludeFilter());
        knownFilters.put("exclude", new ExcludeFilter());
    }
}

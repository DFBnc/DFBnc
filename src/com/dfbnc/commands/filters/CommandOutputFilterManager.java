/*
 * Copyright (c) 2006-2017 DFBnc Developers
 *
 * Where no other license is explicitly given or mentioned in the file, all files
 * in this project are licensed using the following license.
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

package com.dfbnc.commands.filters;

import com.dfbnc.commands.Command;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
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
        knownFilters.put("head", new HeadFilter());
        knownFilters.put("tail", new TailFilter());
        knownFilters.put("count", new CountFilter());
    }
}

/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.util.Util;

/**
 * Filter that excludes all messages that do not match the parameters (i.e., includes only those matching).
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class IncludeFilter implements CommandOutputFilter {

    @Override
    public void runFilter(final String[] params, final CommandOutputBuffer output) throws CommandOutputFilterException {
        output.removeMessagesIf(s -> !s.toLowerCase().matches(".*" + Util.joinString(params, " ").toLowerCase() + ".*"));
    }

}

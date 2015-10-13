/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.CommandOutputBuffer;
import com.dfbnc.util.Util;

/**
 * Filter that excludes any messages matching the parameters.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class ExcludeFilter implements CommandOutputFilter {

    @Override
    public void runFilter(final String[] params, final CommandOutputBuffer output) throws CommandOutputFilterException {
        output.removeMessagesIf(s -> s.toLowerCase().matches(".*" + Util.joinString(params, " ").toLowerCase() + ".*"));
    }

}

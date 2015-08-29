/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.CommandOutput;
import com.dfbnc.util.Util;
import java.util.stream.Collectors;

/**
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class ExcludeFilter implements CommandOutputFilter {
    @Override
    public void runFilter(final String[] params, final CommandOutput output) throws CommandOutputFilterException {
        output.setMessages(output.getMessages().stream().filter(s -> !s.toLowerCase().matches(".*" + Util.joinString(params, " ").toLowerCase() + ".*")).collect(Collectors.toList()));
    }
}

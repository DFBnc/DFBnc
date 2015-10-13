/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.CommandOutputBuffer;
import java.util.Arrays;


/**
 * Filter that shows how many lines there was in the output
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class CountFilter implements CommandOutputFilter {

    @Override
    public void runFilter(final String[] params, final CommandOutputBuffer output) throws CommandOutputFilterException {
        output.setMessages(Arrays.asList(Integer.toString(output.getMessages().size())));
    }

}

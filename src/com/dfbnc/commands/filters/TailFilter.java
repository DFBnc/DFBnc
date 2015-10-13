/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.CommandOutputBuffer;
import java.util.List;

/**
 * Filter that shows only the last X messages
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class TailFilter implements CommandOutputFilter {

    @Override
    public void runFilter(final String[] params, final CommandOutputBuffer output) throws CommandOutputFilterException {
        try {
            final List<String> messages = output.getMessages();
            final int wanted = Integer.parseInt(params.length == 0 ? "10" : params[0]);
            if (wanted < messages.size() && wanted >= 0) {
                output.setMessages(messages.subList(messages.size() - wanted, messages.size()));
            }
        } catch (final NumberFormatException nfe) {
            throw new CommandOutputFilterException("Invalid Number: " + params[0], nfe);
        }
    }

}

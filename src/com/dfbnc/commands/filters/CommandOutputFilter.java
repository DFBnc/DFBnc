/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.commands.filters;

import com.dfbnc.commands.CommandOutputBuffer;

/**
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
@FunctionalInterface
public interface CommandOutputFilter {

    /**
     * Run a filter against command output.
     *
     * @param params Parameters for the filter.
     * @param output This is the CommandOutputBuffer from the command. This may have
     *               already been modified by other filters.
     * @throws CommandOutputFilterException if there was a problem filtering
     *         the output.
     */
    public void runFilter(final String[] params, final CommandOutputBuffer output) throws CommandOutputFilterException;
}

/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

package com.dfbnc.servers.irc;

import com.dfbnc.Account;
import com.dfbnc.servers.logging.ServerLogger;
import com.dmdirc.parser.events.DataOutEvent;
//   - Channel Self Message
//   - Channel Self Action
//   - Channel Self Notice

/**
 * IRC Specific ServerLogger that parses DataOut events into useful events.
 */
public class IRCServerLogger extends ServerLogger {

    /**
     * Create a ServerLogger
     *
     * @param account Account we are logging for.
     * @param connectionHandler IRCConnectionHandler we are logging.
     * @throws Exception if we are unable to create the logs directory.
     */
    public IRCServerLogger(final Account account, final IRCConnectionHandler connectionHandler) throws Exception {
        super(account, connectionHandler);
    }


}

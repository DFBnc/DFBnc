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

package com.dfbnc.util;

import com.dfbnc.sockets.UserSocket;
import java.io.IOException;

/**
 * Writer that sends lines to a UserSocket
 */
public class UserSocketMessageWriter extends ExtendableWriter {

    /** UserSocket to send lines to. */
    private final UserSocket socket;

    /** Bot name to use when sending messages. */
    private final String botName;

    /**
     * Create a new UserSocketMessageWriter with a given capacity.
     *
     * @param socket UserSocket to send lines to
     * @param botName Bot name to use when sending messages.
     */
    public UserSocketMessageWriter(final UserSocket socket, final String botName) {
        super();
        this.socket = socket;
        this.botName = Util.getBotName() + (botName == null || botName.isEmpty() ? "" : "_" + botName);
    }

    @Override
    public void addNewLine(final String line) throws IOException {
        if (!socket.isOpen()) {
            throw new IOException("UserSocket is closed.");
        } else {
            socket.sendLine(":%s!bot@%s PRIVMSG %s :%s", botName, socket.getServerName(), socket.getNickname(), line);
        }
    }
}

/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.util;

import com.dfbnc.sockets.UserSocket;
import java.io.IOException;

/**
 * Writer that sends lines to a UserSocket
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
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

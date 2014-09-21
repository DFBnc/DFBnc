/*
 * Copyright (c) 2006-2013 Shane Mc Cormack
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
package com.dfbnc.commands;

import java.util.LinkedList;
import java.util.List;

import com.dfbnc.sockets.UserSocket;

/**
 * Output from commands is buffered through CommandOutput objects, as this
 * allows output to be filtered.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class CommandOutput {
    /** UserSocket to send output to. */
    private final UserSocket user;

    /** Messages for output. */
    final List<String> messages = new LinkedList<>();

    /**
     * Create a new CommandOutput that will output to the given UserSocket.
     *
     * @param user UserSocket to send output to.
     */
    public CommandOutput(final UserSocket user) {
        this.user = user;
    }

    /**
     * Send a message to the user from the bnc bot in printf format.
     *
     * @param data The format string
     * @param args The args for the format string
     */
    public void sendBotMessage(final String data, final Object... args) {
        messages.add(String.format(data, args));
    }

    /**
     * Get the list of messages.
     *
     * @return This will return a copy of the current messages list.
     */
    public List<String> getMessages() {
        return new LinkedList<>(messages);
    }

    /**
     * Set the list of messages, should only be called by a filter.
     */
    public void setMessages(final List<String> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
    }

    /**
     * Send the output to ths user.
     */
    public void send() {
        for (final String message : messages) {
            user.sendBotMessage(message);
        }
    }
}

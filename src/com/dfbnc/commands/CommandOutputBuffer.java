/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

import com.dfbnc.sockets.UserSocket;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Output from commands is buffered through CommandOutputBuffer objects, as this
 * allows output to be filtered.
 */
public class CommandOutputBuffer {

    /** UserSocket to send output to. */
    private final UserSocket user;

    /** Messages for output. */
    private final List<String> messages = new LinkedList<>();

    /**
     * Create a new CommandOutputBuffer that will output to the given UserSocket.
     *
     * @param user UserSocket to send output to.
     */
    public CommandOutputBuffer(final UserSocket user) {
        this.user = user;
    }

    /**
     * Adds a message to be sent to the user from the bnc bot in printf format.
     *
     * @param data The format string
     * @param args The args for the format string
     */
    public void addBotMessage(final String data, final Object... args) {
        messages.add(args.length == 0 ? data : String.format(data, args));
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
     * Removes all messages that match the specified filter.
     *
     * @param filter The filter to use to decide which messages to remove.
     */
    public void removeMessagesIf(final Predicate<? super String> filter) {
        messages.removeIf(filter);
    }

    /**
     * Send the output to the user.
     */
    public void send() {
        messages.forEach(m -> user.sendBotMessage("%s", m));
    }

}

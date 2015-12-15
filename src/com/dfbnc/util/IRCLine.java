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
package com.dfbnc.util;

import java.util.Map;
import java.util.Map.Entry;

/**
 * This class represents a line of IRC Data with support for message tags.
 * (See: http://ircv3.atheme.org/specification/message-tags-3.2)
 */
public class IRCLine {
    /** The line to send. */
    final String line;

    /** Tags for this line. */
    final Map<String, String> messageTags;

    /**
     * Create a new IRCLine without any message tags.
     *
     * @param line Line to create.
     */
    public IRCLine(final String line) {
        this(line, null);
    }

    /**
     * Create a new IRCLine with some message tags.
     *
     * @param line Line to create.
     * @param messageTags Tags for this line.
     */
    public IRCLine(final String line, final Map<String, String> messageTags) {
        this.line = line;
        this.messageTags = messageTags;
    }

    /**
     * Get a string representation of this line, with any message-tags as
     * required.
     *
     * @return String representation of this line, with any message-tags as
     *         required.
     */
    @Override
    public String toString() {
        if (messageTags == null || messageTags.isEmpty()) { return line; }

        final StringBuilder output = new StringBuilder("@");
        boolean first = true;
        for (final Entry<String,String> s : messageTags.entrySet()) {
            if (!first) { output.append(";"); }
            output.append(s.getKey());
            if (s.getValue() != null && !s.getValue().isEmpty()) {
                output.append("=");
                output.append(s.getValue());
            }
            first = false;
        }

        output.append(" ");
        output.append(line);
        return output.toString();
    }
}

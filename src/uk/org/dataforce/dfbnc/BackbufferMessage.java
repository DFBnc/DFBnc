/*
 * Copyright (c) 2006-2007 Shane Mc Cormack
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

package uk.org.dataforce.dfbnc;

/**
 * This class stores a Backbuffer Message
 *
 * @author shane
 */
public class BackbufferMessage {
    /** The timestamp for this message. */
    private final long time;

    /** The message. */
    private final String message;

    /**
     * Create a new BackbufferMessage.
     *
     * @param time Timestamp.
     * @param message Message.
     */
    public BackbufferMessage(final long time, final String message) {
        this.time = time;
        this.message = message;
    }

    /**
     * Get the message for this message.
     *
     * @return Message for this message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the time for this message.
     *
     * @return Time for this message
     */
    public long getTime() {
        return time;
    }
}

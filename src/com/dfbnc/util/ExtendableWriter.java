/*
 * Copyright (c) 2006-2017 DFBnc Developers
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

import java.io.IOException;
import java.io.Writer;

/**
 * Extendable Writer.
 * Each line added to this writed (delimited by \n) will fire a method that
 * can then do with the messages as it pleases.
 */
public abstract class ExtendableWriter extends Writer {

    /** Current line we are in the process of adding. */
    private final StringBuffer sb = new StringBuffer();

    /**
     * Create a new ExtendableWriter
     */
    public ExtendableWriter() { }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        // Add to stringbuilder
        synchronized (sb) {
            sb.append(cbuf, off, len);
        }
    }

    /**
     * Add a new line to this writer.
     *
     * @param line Line that has been added.
     * @throws IOException If the writer is unable to deal with the message.
     */
    public abstract void addNewLine(final String line)  throws IOException;

    @Override
    public void flush() throws IOException {
        // Store lines.
        synchronized (sb) {
            final String[] bits = sb.toString().split("\n", -1);
            for (int i = 0; i < bits.length-1; i++) {
                addNewLine(bits[i]);
            }
            sb.setLength(0);
            if (bits[bits.length-1].length() > 0) {
                sb.append(bits[bits.length-1]);
            }
        }
    }

    @Override
    public void close() throws IOException { /* Nothing to do. */ }
}

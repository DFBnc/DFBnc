/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Extendable Writer.
 * Each line added to this writed (delimited by \n) will fire a method that
 * can then do with the messages as it pleases.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
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

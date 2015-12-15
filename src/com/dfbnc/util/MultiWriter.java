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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Writer that adds lines to multiple writers.
 * If a writer fails any operation, it will be removed from the list
 * automatically.
 */
public class MultiWriter extends Writer {

    /** List of lines that have been written to this BufferedWriter. */
    private final List<Writer> writers = new LinkedList<>();

    /**
     * Create a new MultiWriter.
     */
    public MultiWriter() { }

    /**
     * Add the given writer to the list of writers.
     * A writer can not be added multiple times.
     *
     * @param writer Writer to add.
     */
    public synchronized void addWriter(final Writer writer) {
        if (writers.contains(writer)) { return; }
        writers.add(writer);
    }

    /**
     * Remove the given writer from the list of writers.
     * NOTE: This will not close the writer.
     *
     * @param writer Writer to add.
     */
    public synchronized void removeWriter(final Writer writer) {
        writers.remove(writer);
    }

    @Override
    public synchronized void write(char[] cbuf, int off, int len) throws IOException {
        for (final Iterator<Writer> i = writers.iterator(); i.hasNext(); ) {
            final Writer w = i.next();
            try {
                w.write(cbuf, off, len);
            } catch (final IOException ioe) {
                i.remove();
            }
        }
    }

    @Override
    public synchronized void flush() throws IOException {
        for (final Iterator<Writer> i = writers.iterator(); i.hasNext(); ) {
            final Writer w = i.next();
            try {
                w.flush();
            } catch (final IOException ioe) {
                i.remove();
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        for (final Iterator<Writer> i = writers.iterator(); i.hasNext(); ) {
            final Writer w = i.next();
            try {
                w.close();
            } catch (final IOException ioe) {
                i.remove();
            }
        }
    }


}

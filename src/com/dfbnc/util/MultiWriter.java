/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.util;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

/**
 * Writer that adds lines to multiple writers.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
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
     *
     * @param writer Writer to add.
     */
    public void addWriter(final Writer writer) {
        writers.add(writer);
    }

    /**
     * Remove the given writer from the list of writers.
     * NOTE: This will not close the writer.
     *
     * @param writer Writer to add.
     */
    public void removeWriter(final Writer writer) {
        writers.remove(writer);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (final Writer w : writers) { w.write(cbuf, off, len); }
    }

    @Override
    public void flush() throws IOException {
        for (final Writer w : writers) { w.flush(); }
    }

    @Override
    public void close() throws IOException {
        for (final Writer w : writers) { w.close(); }
    }


}

/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
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

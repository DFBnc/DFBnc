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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Writer that stores lines in a RollingList.
 * This also implements List<String> to allow the contents to be traversed.
 * "Add" and "Remove" type methods (with the exception of clear()) from List
 * will throw an UnsupportedOperationException as all new lines must be added
 * by the Writer interface.
 */
public class RollingWriter extends ExtendableWriter implements List<String> {

    /** List of lines that have been written to this BufferedWriter. */
    private final RollingList<String> storedLines;

    /**
     * Create a new RollingdWriter with a given capacity.
     *
     * @param capacity Number of lines to allow
     */
    public RollingWriter(final int capacity) {
        super();
        storedLines = new RollingList<>(capacity);
    }

    /**
     * Get the current capacity of this list.
     *
     * @return Current capacity of this list.
     */
    public int getCapacity() {
        return storedLines.getCapacity();
    }

    /**
     * Set the current capacity of this list.
     *
     * @param newValue New capacity for this list,
     */
    public void setCapacity(final int newValue) {
        storedLines.setCapacity(newValue);
    }

    @Override
    public void addNewLine(final String line) throws IOException  {
        storedLines.add(line);
    }

    @Override
    public int size() { return storedLines.size(); }

    @Override
    public boolean isEmpty() { return storedLines.isEmpty(); }

    @Override
    public boolean contains(final Object o) { return storedLines.contains(o); }

    @Override
    public Iterator<String> iterator() { return storedLines.iterator(); }

    @Override
    public Object[] toArray() { return storedLines.toArray(); }

    @Override
    public <T> T[] toArray(final T[] a) { return storedLines.toArray(a); }

    @Override
    public boolean add(final String e) { throw new UnsupportedOperationException("Not supported, use write()."); }

    @Override
    public boolean remove(final Object o) { throw new UnsupportedOperationException("Not supported."); }

    @Override
    public boolean containsAll(final Collection<?> c) { return storedLines.containsAll(c); }

    @Override
    public boolean addAll(final Collection<? extends String> c) { throw new UnsupportedOperationException("Not supported, use write()."); }

    @Override
    public boolean addAll(final int index, Collection<? extends String> c) { throw new UnsupportedOperationException("Not supported, use write()."); }

    @Override
    public boolean removeAll(final Collection<?> c) { throw new UnsupportedOperationException("Not supported."); }

    @Override
    public boolean retainAll(final Collection<?> c) { throw new UnsupportedOperationException("Not supported."); }

    @Override
    public void clear() { storedLines.clear(); }

    @Override
    public String get(final int index) { return storedLines.get(index); }

    @Override
    public String set(final int index, final String element) { throw new UnsupportedOperationException("Not supported."); }

    @Override
    public void add(final int index, final String element) { throw new UnsupportedOperationException("Not supported, use write()."); }

    @Override
    public String remove(final int index) { throw new UnsupportedOperationException("Not supported."); }

    @Override
    public int indexOf(final Object o) { return storedLines.indexOf(o); }

    @Override
    public int lastIndexOf(final Object o) { return storedLines.lastIndexOf(o); }

    @Override
    public ListIterator<String> listIterator() { return storedLines.listIterator(); }

    @Override
    public ListIterator<String> listIterator(final int index) { return storedLines.listIterator(index); }

    @Override
    public List<String> subList(final int fromIndex, final int toIndex) { return storedLines.subList(fromIndex, toIndex); }


}

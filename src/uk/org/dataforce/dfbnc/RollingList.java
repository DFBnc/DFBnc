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

import java.util.LinkedList;
import java.util.Collection;

/**
 * Implements a simple rolling list. As newer items are added beyond the
 * capacity of the list, items from the *start of the list* are removed to keep
 * the list under-capacity.
 *
 * @param <T> Type of items to add
 */
public class RollingList<T> extends LinkedList<T> {
    
    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialised
     * objects being un-serialised with the new class).
     */
    private static final long serialVersionUID = 1L;
    
    /** Capacity of the list */
    private int capacity;
    
    /**
     * Creates a new RollingList with the specified capacity.
     *
     * @param capacity The capacity for this list.
     */
    public RollingList(final int capacity) {
        super();
        this.capacity = Math.max(0, capacity);
    }
    
    /**
     * Get the current capacity of this list.
     * 
     * @return Current capacity of this list.
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Set the current capacity of this list.
     * 
     * @param newValue New capacity for this list,
     */
    public void setCapacity(final int newValue) {
        capacity = Math.max(0, newValue);
        pruneList(capacity);
    }
    
    /**
     * Prune the start of the list to keep the capacity at the given value.
     *
     * @param max Maximum elements to keep.
     */
    private void pruneList(final int max) {
        while (size() > max) {
            pop();
        }
    }
    
    //=========================================================================
    // Below are implementations of any method that adds anything to this list.
    //=========================================================================

    /** {@inheritDoc} */
    @Override
    public boolean add(final T element) {
        super.add(element);
        pruneList(capacity);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(final Collection<? extends T> c) {
        super.addAll(c);
        pruneList(capacity);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void add(final int index, final T element) {
        super.add(index, element);
        pruneList(capacity);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(final int index, Collection<? extends T> c) {
        super.addAll(index, c);
        pruneList(capacity);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void addFirst(final T e) {
        super.addFirst(e);
        pruneList(capacity);
    }

    /** {@inheritDoc} */
    @Override
    public void addLast(final T e) {
        super.addLast(e);
        pruneList(capacity);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean offer(final T e) {
        super.offer(e);
        pruneList(capacity);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean offerFirst(final T e) {
        super.offerFirst(e);
        pruneList(capacity);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean offerLast(final T e) {
        super.offerLast(e);
        pruneList(capacity);
        return true;
    }

    @Override
    public void push(final T e) {
        super.push(e);
        pruneList(capacity);
    }
}

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
package uk.org.dataforce.dfbnc.sockets;

import java.util.concurrent.Semaphore;

/**
 * Synchronized wrapper around StringBuffer used to buffer output to a socket.
 * 
 * @author shane
 */
public class OutputBuffer {
    /** Semaphore for synchronization */
    private final Semaphore mySemaphore = new Semaphore(1);
    
    /** Lines to be sent to the user go into this buffer. */
    protected final StringBuffer buffer = new StringBuffer();
    
    /**
     * Add data to output buffer.
     * 
     * @param args Strings to add to the output buffer.
     */
    public void addData(final String... args) {
        mySemaphore.acquireUninterruptibly();
        for (final String data : args) {
            buffer.append(data);
        }
        mySemaphore.release();
    }
    
    /**
     * Check if the output buffer has data in it or not.
     * 
     * @return True if there is data in the output buffer
     */
    public boolean hasData() {
        try {
            mySemaphore.acquire();
        } catch (final InterruptedException ie) {
            // Thread was interrupted when trying to get the buffer.
            // Assuming we have no data seems the least problematic, as
            // chances are we will be asked again in future.
            return false;
        }
        
        try {
            return buffer.length() > 0;
        } finally {
            mySemaphore.release();
        }
    }
    
    /**
     * Get the current output buffer content and reset the stored buffer.
     * 
     * @return The data that was in the output buffer
     */
    public String getAndReset() {
        try {
            mySemaphore.acquire();
        } catch (final InterruptedException ie) {
            // Thread was interrupted when trying to get the buffer.
            // Doing nothing seems the least damaging.
            return "";
        }
        
        try {
            final String result = buffer.toString();
            buffer.setLength(0);
            return result;
        } finally {
            mySemaphore.release();
        }
    }
}

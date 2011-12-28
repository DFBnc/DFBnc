/*
 * Copyright (c) 2006-2009 Shane Mc Cormack, Gregory Holmes
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

package uk.org.dataforce.dfbnc.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Configuration Files.
 */
public class BlackHoleConfig extends Config {
    /**
     * Creates a new BlackHole config.
     * This config will discard all attempts to write data, and will always
     * return fallback values.
     */
    private BlackHoleConfig() throws IOException, InvalidConfigFileException {
        super(new ByteArrayInputStream("".getBytes()));
    }
    
    /**
     * Create a new BlackHole config.
     */
    public static BlackHoleConfig createInstance() {
        try {
            return new BlackHoleConfig();
        } catch (final Exception e) {
            /* We never do anything with the underlying stream, so ignore. */
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
     * Get an option from the config
     *
     * @param domain Domain for option
     * @param key key for option
     * @param fallback Value to return if key is not found
     * @return The fallback value given.
     */
    @Override
    public String getOption(final String domain, final String key, final String fallback) {
        return fallback;
    }

    /**
     * Does nothing.
     *
     * @param domain Domain for option
     * @param key key for option
     * @param value Value for option
     */
    @Override
    public void setOption(final String domain, final String key, final String value) { }

    /**
     * Always returns true.
     *
     * @param domain Domain for option
     * @param key key for option
     * @return True
     */
    @Override
    public boolean hasOption(final String domain, final String key) {
        return true;
    }

    /**
     * Does nothing.
     */
    @Override
    public void save() { }
}

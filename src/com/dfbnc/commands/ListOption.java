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

package com.dfbnc.commands;

/**
 * This is used by AbstractListEditCommand.checkItem to validate an item.
 */
public class ListOption {

    /** Is the item valid? */
    private final boolean isValid;
    /** How the item should be stored in the list */
    private final String param;
    /** Output to give if param is invalid */
    private final String[] output;

    /**
     * Creates a new list option.
     *
     * @param isValid Valid item
     * @param param Parameter
     * @param output Output
     */
    public ListOption(final boolean isValid, final String param, final String[] output) {
        super();
        this.isValid = isValid;
        this.param = param;
        if (output == null) {
            this.output = null;
        } else {
            this.output = output.clone();
        }
    }

    /**
     * Get the value of isValid
     *
     * @return value of isValid
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Get the value of param
     *
     * @return value of param
     */
    public String getParam() {
        return param;
    }

    /**
     * Get the value of output
     *
     * @return value of output
     */
    public String[] getOutput() {
        if (output == null) {
            return null;
        } else {
            return output.clone();
        }
    }
}

/*
 * Copyright (c) 2006-2016 DFBnc Developers
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
package com.dfbnc.authentication;

import com.dfbnc.sockets.UserSocket;

/**
 * Auth provider base class.
 */
public abstract class AuthProvider {
    /**
     * Name of this provider.
     *
     * This needs to be a single word as it is used as the method name in the authlist
     *
     * @return Name of provider
     */
    public abstract String getProviderName();

    /**
     * Get description of params expected by validateParams.
     *
     * @return Description of expected params
     */
    public abstract String getExpectedParams();

    /**
     * Test to see if authentication passes.
     *
     * @param user UserSocket to check
     * @param test Parameters String to test against
     * @return True if authentication passes.
     */
    public abstract boolean checkAuthentication(final UserSocket user, final String test);

    /**
     * Validate input to see if it can be added to the authlist.
     * This also allows the provider to make changes if required before saving.
     * (Such as hashing a password)
     *
     * @param user UserSocket that wants to validate the input
     * @param subClientID SubClientID that this is being set for
     * @param input Parameters String to add
     * @return String to add after validation, or "" if not valid.
     */
    public abstract String validateParams(final UserSocket user, final String subClientID, final String input);

}

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
package com.dfbnc.authentication.providers;

import com.dfbnc.authentication.AuthProvider;
import com.dfbnc.sockets.UserSocket;

/**
 * Provider that validates Client SSL Certificates for authentication.
 */
public class ClientCertProvider extends AuthProvider {

    @Override
    public String getProviderName() {
        return "CLIENTCERT";
    }

    @Override
    public String getExpectedParams() {
        return "[FINGERPRINT]";
    }

    @Override
    public boolean checkAuthentication(final UserSocket user, final String test) {
        return user.getClientCertFP().equals(test);
    }

    @Override
    public String validateParams(final UserSocket user, final String subClientID, final String input) {
        if (input.isEmpty()) {
            return user.getClientCertFP();
        } else {
            return input;
        }
    }

}

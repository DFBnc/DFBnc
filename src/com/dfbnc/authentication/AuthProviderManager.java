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

import java.util.HashMap;
import java.util.Map;
import uk.org.dataforce.libs.logger.Logger;

/**
 * Authentication Provider Manager.
 */
public class AuthProviderManager {
    private final Map<String, AuthProvider> providers = new HashMap<>();

    /**
     * Add a provider to this manager.
     *
     * @param provider Provider to add.
     */
    public void addProvider(final AuthProvider provider) {
        providers.put(provider.getProviderName().toUpperCase(), provider);
        Logger.debug("Adding AuthProvider: " + provider.getProviderName());
    }

    /**
     * Get a map of all providers.
     *
     * @return Map of providers.
     */
    public Map<String, AuthProvider> getProviders() {
        return new HashMap<>(providers);
    }

    /**
     * Get the provider with the given name.
     *
     * @param name Name to get provider for.
     * @return Provider or Null.
     */
    public AuthProvider getProvider(final String name) {
        return providers.get(name.toUpperCase());
    }

    /**
     * Is there a provider with the given name?
     *
     * @param name Name to get provider for.
     * @return True if there is a provider with this name.
     */
    public boolean hasProvider(final String name) {
        return providers.containsKey(name.toUpperCase());
    }
}

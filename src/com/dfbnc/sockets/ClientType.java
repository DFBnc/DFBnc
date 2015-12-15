/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

package com.dfbnc.sockets;

/**
 * Type of client connected.
 */
public enum ClientType {
    Generic("Generic IRC Client"),
    irssi("Irssi"),
    DMDirc("DMDirc"),
    mIRC("mIRC"),
    TapChat("TapChat");

    /** Description of this ClientType. */
    private final String description;

    /**
     * Create a new ClientType with a description.
     *
     * @param description Description
     */
    ClientType(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Get a ClientType object based on a version string.
     *
     * @param clientVersion Version string to parse.
     * @return ClientType for the given version, or Generic as a fallback.
     */
    public static ClientType getFromVersion(final String clientVersion) {
        if (clientVersion != null) {
            if (clientVersion.toLowerCase().contains("dmdirc")) {
                return DMDirc;
            } else if (clientVersion.toLowerCase().contains("tapchat")) {
               return TapChat;
            } else if (clientVersion.toLowerCase().contains("irssi")) {
                return irssi;
            } else if (clientVersion.toLowerCase().contains("mirc")) {
                return mIRC;
            }
        }

        return Generic;
    }

    /**
     * Get a ClientType object based on a given name.
     *
     * @param name Name string to parse.
     * @return ClientType for the given name, or Generic as a fallback.
     */
    public static ClientType getFromName(final String name) {
        for (ClientType ct : ClientType.values()) {
          if (ct.name().equalsIgnoreCase(name)) {
            return ct;
          }
        }

        return Generic;
    }
}

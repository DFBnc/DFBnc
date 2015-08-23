/*
 *  Copyright 2015 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */

package com.dfbnc.sockets;

/**
 * Type of client connected.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
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

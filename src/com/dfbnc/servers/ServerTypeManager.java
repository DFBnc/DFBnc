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
package com.dfbnc.servers;

import java.util.ArrayList;
import com.dfbnc.servers.irc.IRCServerType;
import uk.org.dataforce.libs.logger.Logger;
import java.util.Collection;
import java.util.HashMap;

/**
 * DFBNC ServerType Manager.
 * Manages adding/removing/creating ServerTypes.
 *
 * @author Shane Mc Cormack
 * @version $Id: ServerTypeManager.java 1360 2007-05-25 19:12:05Z ShaneMcC $
 */
public final class ServerTypeManager {
    /** HashMap used to store the different types of ServerType known. */
    private HashMap<String,ServerType> knownServerTypes = new HashMap<String,ServerType>();

    /**
     * Constructor to create a ServerTypeManager
     */
    public ServerTypeManager() { }

    /**
     * Initialise the ServerTypeManager with the default ServerTypes
     */
    public void init() {
        //------------------------------------------------
        // Add ServerTypes
        //------------------------------------------------
        addServerType(new String[]{"IRC"}, new IRCServerType(this));
    }

    /**
     * Remove all ServerTypes
     */
    public void empty() {
        knownServerTypes.clear();
    }

    /**
     * Empty clone method to prevent cloning to get more copies of the ServerTypeManager
     *
     * @throws CloneNotSupportedException Always
     * @return Nothing.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Add a ServerType type
     *
     * @param names String Array of valid names for this ServerType
     * @param serverType ServerType subclass for the ServerType.
     */
    public void addServerType(final String[] names, final ServerType serverType) {
        Logger.debug("Adding ServerType: "+serverType.getName());

        try {
            for (int i = 0; i < names.length; ++i) {
                if (knownServerTypes.containsKey(names[i].toLowerCase())) {
                    knownServerTypes.remove(names[i].toLowerCase());
                }
                Logger.debug2("\t Added ServerType for: "+names[i]);
                knownServerTypes.put(names[i].toLowerCase(), serverType);
            }
        } catch (Exception e) {
            Logger.error("Error adding ServerType '"+serverType.toString()+"': "+e.getMessage());
            delServerType(serverType);
        }
    }

    /**
     * Remove a ServerType type.
     *
     * @param serverType ServerType subclass for the ServerType.
     */
    public void delServerType(final ServerType serverType) {
        ServerType testServerType;
        Logger.debug("Deleting ServerType: "+serverType.getName());
        for (String elementName : knownServerTypes.keySet()) {
            Logger.debug2("\t Checking handler for: "+elementName);
            testServerType = knownServerTypes.get(elementName);
            if (testServerType.getName().equalsIgnoreCase(serverType.getName())) {
                Logger.debug2("\t Removed handler for: "+elementName);
                knownServerTypes.remove(elementName);
            }
        }
    }

    /**
     * Get the ServerType type of a given name
     *
     * @param name Name to look for
     * @return ServerType for the given name.
     * @throws ServerTypeNotFound If the requested serverType does not exist.
     */
    public ServerType getServerType(final String name) throws ServerTypeNotFound {
        if (knownServerTypes.containsKey(name.toLowerCase())) {
            return knownServerTypes.get(name.toLowerCase());
        } else {
            throw new ServerTypeNotFound("No ServerType is known by "+name);
        }
    }

    /**
     * Get the valid ServerTypes
     *
     * @return Valid ServerTypes as a collection
     */
    public Collection<ServerType> getServerTypes() {
        return new ArrayList<ServerType>(knownServerTypes.values());
    }

    /**
     * Get the valid ServerTypes Names
     *
     * @return Valid ServerType Names as a collection
     */
    public Collection<String> getServerTypeNames() {
        return new ArrayList<String>(knownServerTypes.keySet());
    }
}


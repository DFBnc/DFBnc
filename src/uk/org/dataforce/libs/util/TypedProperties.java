/*
 * Copyright (c) 2006-2008 Shane Mc Cormack
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
 *
 * SVN: $Id$
 */
package uk.org.dataforce.libs.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;

/**
 * Properties file that allows for getting/setting of typed properties
 */
public class TypedProperties extends Properties {
    /**
     * A version number for this class.
     * It should be changed whenever the class structure is changed (or anything
     * else that would prevent serialized objects being unserialized with the new 
     * class).
     */
    private static final long serialVersionUID = 200711071;
    
    /** Is this properties file Case Sensitive */
    private boolean caseSensitive = true;
    
    /**
     * Creates an empty property list with no default values.
     */
    public TypedProperties() {
        super();
    }
    
    /**
     * Creates an empty property list with the specified defaults.
     *
     * @param defaults The Defaults
     */
    public TypedProperties(final Properties defaults) {
        super(defaults);
    }
    
    /**
     * Set case sensitivity of this properties file.
     *
     * @param value True/False for the case sensitivity of this file
     */
    public void setCaseSensitivity(final boolean value) {
        // Set all existing values to lowercase.
        if (!value) {
            for (Object property : this.keySet()) {
                if (property instanceof String) {
                    final String propertyName = (String)property;
                    if (!propertyName.equals(propertyName.toLowerCase())) {
                        super.setProperty(propertyName.toLowerCase(), getProperty(propertyName));
                        super.remove(propertyName);
                    }
                }
            }
        }
        caseSensitive = value;
    }
    
    /**
     * Load properties from an InputStream.
     * After loading, setCaseSensitivity(caseSensitive) is called.
     * If this properties file is ment to be case Insensitive, all non-lowercase
     * property names will be lowercased.
     *
     * @param inStream InputStream to load from.
     * @throws IOException If there is a problem reading from the Input Stream
     */
    @Override
    public void load(final InputStream inStream) throws IOException {
        super.load(inStream);
        setCaseSensitivity(caseSensitive);
    }
    
    /**
     * Load properties from a Reader.
     * After loading, setCaseSensitivity(caseSensitive) is called.
     * If this properties file is ment to be case Insensitive, all non-lowercase
     * property names will be lowercased.
     *
     * @param reader Reader to load from.
     * @throws IOException If there is an error reading from the reader
     */
    @Override
    public void load(final Reader reader) throws IOException {
        super.load(reader);
        setCaseSensitivity(caseSensitive);
    }
    
    /**
     * Load properties from an XML InputStream.
     * After loading, setCaseSensitivity(caseSensitive) is called.
     * If this properties file is ment to be case Insensitive, all non-lowercase
     * property names will be lowercased.
     *
     * @param in InputStream to load from.
     * @throws java.io.IOException 
     */
    @Override
    public void loadFromXML(final InputStream in) throws IOException {
        super.loadFromXML(in);
        setCaseSensitivity(caseSensitive);
    }
    
    /**
     * Get a property from the config
     *
     * @param key key for property
     * @return the requested property, or null if not defined
     */
    @Override
    public String getProperty(final String key) {
        if (!caseSensitive) {
            return super.getProperty(key.toLowerCase());
        } else {
            return super.getProperty(key);
        }
    }
    
    /**
     * Get a property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    @Override
    public String getProperty(final String key, final String fallback) {
        if (!caseSensitive) {
            return super.getProperty(key.toLowerCase(), fallback);
        } else {
            return super.getProperty(key, fallback);
        }
    }
    
    /**
     * Set a property in the config
     *
     * @param key key for property
     * @param value Value for property
     * @return Old value of property
     */
    @Override
    public Object setProperty(final String key, final String value) {
        if (!caseSensitive) {
            return super.setProperty(key.toLowerCase(), value);
        } else {
            return super.setProperty(key, value);
        }
    }
    
    /**
     * Check if a property exists
     *
     * @param key key for property
     * @return True if the property exists, else false
     */
    public boolean hasProperty(final String key) {
        return getProperty(key) != null;
    }
    
    /**
     * Get a Byte property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public byte getByteProperty(final String key, final byte fallback) {
        try {
            return Byte.parseByte(getProperty(key, Byte.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }
    
    /**
     * Set a Byte property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setByteProperty(final String key, final byte value) {
        setProperty(key, Byte.toString(value));
    }
    
    /**
     * Get a Short property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public short getShortProperty(final String key, final short fallback) {
        try {
            return Short.parseShort(getProperty(key, Short.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }
    
    /**
     * Set a Short property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setShortProperty(final String key, final short value) {
        setProperty(key, Short.toString(value));
    }
    
    /**
     * Get an integer property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public int getIntProperty(final String key, final int fallback) {
        try {
            return Integer.parseInt(getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }
    
    /**
     * Set an integer property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setIntProperty(final String key, final int value) {
        setProperty(key, Integer.toString(value));
    }
    
    /**
     * Get a Long property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public long getLongProperty(final String key, final long fallback) {
        try {
            return Long.parseLong(getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }
    
    /**
     * Set a Long property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setLongProperty(final String key, final long value) {
        setProperty(key, Long.toString(value));
    }
    
    /**
     * Get a float property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public float getFloatProperty(final String key, final float fallback) {
        try {
            return Float.parseFloat(getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }
    
    /**
     * Set a float property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setFloatProperty(final String key, final float value) {
        setProperty(key, Float.toString(value));
    }
    
    /**
     * Get a double property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public double getDoubleProperty(final String key, final double fallback) {
        try {
            return Double.parseDouble(getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }
    
    /**
     * Set a double property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setDoubleProperty(final String key, final double value) {
        setProperty(key, Double.toString(value));
    }
    
    /**
     * Get a boolean property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
      * @return the requested property, or the fallback value if not defined
     */
    public boolean getBoolProperty(final String key, final boolean fallback) {
        return Boolean.parseBoolean(getProperty(key, Boolean.toString(fallback)));
    }
    
    /**
     * Set a Boolean property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setBoolProperty(final String key, final boolean value) {
        setProperty(key, Boolean.toString(value));
    }
    
    /**
     * Get a Char property from the config
     *
     * @param key key for property
     * @param fallback Value to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public char getCharProperty(final String key, final char fallback) {
        final String res = getProperty(key, Character.toString(fallback));
        if (res == null || res.isEmpty()) {
            return fallback;
        } else {
            return res.charAt(0);
        }
    }
    
    /**
     * Set a Char property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setCharProperty(final String key, final char value) {
        setProperty(key, Character.toString(value));
    }
    
    /**
     * Get a List property from the config.
     *
     * @param key key for property
     * @param fallback List to return if key is not found
     * @return the requested property, or the fallback value if not defined
     */
    public List<String> getListProperty(final String key, final List<String> fallback) {
        final String res = getProperty(key, "");
        if (res == null || res.isEmpty()) {
            return fallback;
        } else {
            final String bits[] = res.split("\n");
            final ArrayList<String> result = new ArrayList<String>();
            for (String bit : bits) {
                result.add(bit);
            }
            return result;
        }
    }
    
    /**
     * Set a List property in the config
     *
     * @param key key for property
     * @param value Value for property
     */
    public void setListProperty(final String key, final List<String> value) {
        final StringBuilder val = new StringBuilder();
        final String LF = "\n";
        boolean first = true;
        for (String bit : value) {
            if (first) { first = false; } else { val.append(LF); }
            val.append(bit);
        }
        setProperty(key, val.toString());
    }
}

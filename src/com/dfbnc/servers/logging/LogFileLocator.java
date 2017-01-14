/*
 * Copyright (c) 2006-2017 DFBnc Developers
 * Copyright (c) 2006-2015 DMDirc Developers
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

package com.dfbnc.servers.logging;

import com.dfbnc.Account;
import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;


/**
 * Facilitates finding a path for log files.
 *
 * This class is based on parts of the DMDirc Logging Plugin.
 */
public class LogFileLocator {

    /** Whether to append a hash of the file name to the file name. */
    private final boolean filenamehash = false;

    /** Whether to use date formats in file names. */
    private final boolean usedate = false;

    /** Date format to use in file names if {@link #usedate} is true. */
    private final String usedateformat = "yyyy-MM-dd";

    /** The account we are logging for. */
    private final Account myAccount;

    public LogFileLocator(final Account account) throws Exception {
        myAccount = account;

        final File dir = new File(myAccount.getConfigDirectory(), "logs");
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new Exception("Unable to create logging dir (file exists instead)");
            }
        } else {
            if (!dir.mkdirs()) {
                throw new Exception("Unable to create logging dir");
            }
        }
    }

    /**
     * Sanitises the log file directory.
     *
     * @return Log directory
     */
    private StringBuffer getLogDirectory() {
        final StringBuffer directory = new StringBuffer();
        directory.append(new File(myAccount.getConfigDirectory(), "logs").toString());
        if (directory.charAt(directory.length() - 1) != File.separatorChar) {
            directory.append(File.separatorChar);
        }
        return directory;
    }

    /**
     * Get the name of the log file for a specific object.
     *
     * @param channel Channel to get the name for
     *
     * @return the name of the log file to use for this object.
     */
    public String getLogFile(final ChannelInfo channel) {
        final StringBuffer directory = getLogDirectory();
        final StringBuffer file = new StringBuffer();
        file.append(sanitise(channel.getName().toLowerCase()));
        return getPath(directory, file, channel.getName());
    }

    /**
     * Get the name of the log file for a specific object.
     *
     * @param user Client to get the name for
     *
     * @return the name of the log file to use for this object.
     */
    public String getLogFile(final ClientInfo user) {
        final StringBuffer directory = getLogDirectory();
        final StringBuffer file = new StringBuffer();
        file.append(sanitise(user.getNickname().toLowerCase()));
        return getPath(directory, file, user.getNickname());
    }

    /**
     * Get the name of the log file for a specific object.
     *
     * @param descriptor Description of the object to get a log file for.
     *
     * @return the name of the log file to use for this object.
     */
    public String getLogFile(final String descriptor) {
        final StringBuffer directory = getLogDirectory();
        final StringBuffer file = new StringBuffer();
        final String md5String;
        if (descriptor == null) {
            file.append("null.log");
            md5String = "";
        } else {
            file.append(sanitise(descriptor.toLowerCase()));
            md5String = descriptor;
        }
        return getPath(directory, file, md5String);
    }

    /**
     * Gets the path for the given file and directory. Only intended to be used from getLogFile
     * methods.
     *
     * @param directory Log file directory
     * @param file      Log file path
     * @param md5String Log file object MD5 hash
     *
     * @return Name of the log file
     */
    public String getPath(final StringBuffer directory, final StringBuffer file, final String md5String) {
        if (usedate) {
            final String dateFormat = usedateformat;
            final String dateDir = new SimpleDateFormat(dateFormat).format(new Date());
            directory.append(dateDir);
            if (directory.charAt(directory.length() - 1) != File.separatorChar) {
                directory.append(File.separatorChar);
            }

            if (!new File(directory.toString()).exists() && !new File(directory.toString()).mkdirs()) {
                return null;
            }
        }

        if (filenamehash) {
            file.append('.');
            file.append(md5(md5String));
        }
        file.append(".log");

        return directory + file.toString();
    }

    /**
     * Sanitise a string to be used as a filename.
     *
     * @param name String to sanitise
     *
     * @return Sanitised version of name that can be used as a filename.
     */
    protected static String sanitise(final String name) {
        // Replace illegal chars with
        return name.replaceAll("[^\\w\\.\\s\\-#&_]", "_");
    }

    /**
     * Get the md5 hash of a string.
     *
     * @param string String to hash
     *
     * @return md5 hash of given string
     */
    protected static String md5(final String string) {
        try {
            final MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(string.getBytes(), 0, string.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

}
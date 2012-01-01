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

package uk.org.dataforce.libs.logger;

import java.io.BufferedWriter;

/**
 * JISG Logger class.
 */
public class Logger {
    /** Current log level. */
    private static LogLevel logLevel = LogLevel.DEBUG2;

    /** Optional Tag for log entries. */
    private static String logTag = "";

    /** Optional BufferedWriter to write output to in addition to console. */
    private static BufferedWriter writer = null;

    /**
     * Change the logTag for this logger.
     *
     * @param newTag New tag to use.
     */
    public static void setTag(final String newTag) {
        logTag = (newTag == null) ? "" : newTag;
    }

    /**
     * Get the current logTag for this logger.
     *
     * @return The current log tag.
     */
    public static String getTag() {
        return logTag;
    }

    /**
     * Set a buffered writer for the logger to also log data to.
     * The Application (not Logger) is responsible for opening this and closing
     * it when no longer required.
     * Setting this to null will disable writing.
     * Logger will make no attempt to ensure that writing actually succeeds.
     *
     * @param newWriter New writer to use.
     */
    public static void setWriter(final BufferedWriter newWriter) {
        writer = newWriter;
    }

    /**
     * Get the current bufferedwriter we are using.
     *
     * @return The writer we are using.
     */
    public static BufferedWriter getWriter() {
        return writer;
    }

    /**
     * Log data at a customiseable log level.
     *
     * @param level Level of this information.
     * @param data Information to log.
     */
    public static void log(final LogLevel level, final String data) {
        if (level.isLoggable(logLevel) && level != LogLevel.SILENT) {
            final String output = data == null ? "" : String.format("[%s%s] %s", (logTag.isEmpty() ? "" : logTag + ":"), level, data);

            System.out.println(output);
            if (writer != null) {
                try {
                    writer.append(output);
                    writer.append("\n");
                    writer.flush();
                } catch (final Exception e) {
                    // Writing failed, so abandon writer!
                    // This stops us trying this again every line.
                    writer = null;
                }
            }
        }
    }

    /**
     * Log data at the error level.
     *
     * @param data Information to log.
     */
    public static void error(final String data) {
        log(LogLevel.ERROR, data);
    }

    /**
     * Log data at the warning level.
     *
     * @param data Information to log.
     */
    public static void warning(final String data) {
        log(LogLevel.WARNING, data);
    }

    /**
     * Log data at the info level.
     *
     * @param data Information to log.
     */
    public static void info(final String data) {
        log(LogLevel.INFO, data);
    }

    /**
     * Log data at the debug level.
     *
     * @param data Information to log.
     */
    public static void debug(final String data) {
        log(LogLevel.DEBUG, data);
    }

    /**
     * Log data at the debug2 level.
     *
     * @param data Information to log.
     */
    public static void debug2(final String data) {
        log(LogLevel.DEBUG2, data);
    }

    /**
     * Log data at the debug3 level.
     *
     * @param data Information to log.
     */
    public static void debug3(final String data) {
        log(LogLevel.DEBUG3, data);
    }

    /**
     * Log data at the debug4 level.
     *
     * @param data Information to log.
     */
    public static void debug4(final String data) {
        log(LogLevel.DEBUG4, data);
    }

    /**
     * Log data at the debug5 level.
     *
     * @param data Information to log.
     */
    public static void debug5(final String data) {
        log(LogLevel.DEBUG5, data);
    }

    /**
     * Log data at the debug6 level.
     *
     * @param data Information to log.
     */
    public static void debug6(final String data) {
        log(LogLevel.DEBUG6, data);
    }

    /**
     * Log data at the debug7 level.
     *
     * @param data Information to log.
     */
    public static void debug7(final String data) {
        log(LogLevel.DEBUG7, data);
    }

    /**
     * Log data at the debug8 level.
     *
     * @param data Information to log.
     */
    public static void debug8(final String data) {
        log(LogLevel.DEBUG8, data);
    }

    /**
     * Log data at the debug9 level.
     *
     * @param data Information to log.
     */
    public static void debug9(final String data) {
        log(LogLevel.DEBUG9, data);
    }

    /**
     * Get the current log level.
     * Any data above the current log level will not be logged.
     *
     * @return The current LogLevel.
     */
    public static LogLevel getLevel() {
        return logLevel;
    }

    /**
     * Set the current log level.
     * Any data above the current log level will not be logged.
     *
     * @param level The new LogLevel.
     */
    public static void setLevel(final LogLevel level) {
        logLevel = level;
        debug2("LogLevel changed to: "+level);
    }

    /** Prevent instances of Logger */
    private Logger() {    }
}

/*
 * Copyright (c) 2006-2015 DFBnc Developers
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
package com.dfbnc;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;

import static com.sun.akuma.CLibrary.LIBC;

/**
 * This class extends the Daemon class for DFBnc specific requirements.
 */
public class DFBncDaemon extends Daemon {
    /** Is forking supported? */
    private static boolean canFork = checkCanFork();

    /**
     * Prepares the current process to act as a daemon.
     * This version doesn't call closeDescriptors(), this will need to be done
     * manually, and doesn't chdir to root.
     *
     * @param pidFile the filename to which the daemon's PID is written or null;
     */
    @SuppressWarnings({"OctalInteger"})
    @Override
    public void init(final String pidFile) throws Exception {
        // start a new process session
        LIBC.setsid();
        if (pidFile != null) { writePidFile(pidFile); }
    }

    @Override
    public void closeDescriptors() throws IOException {
        super.closeDescriptors();
    }

    /**
     * Get the arguments used to call this process.
     *
     * @return Current Arguments
     */
    public static List<String> getArgs() {
        try {
            return JavaVMArguments.of(getPID());
        } catch (final Throwable t) {
            return Arrays.asList(new String[0]);
        }
    }

    /**
     * Get the current PID.
     *
     * @return Current PID as an int.
     */
    public static int getPID() {
        return LIBC.getpid();
    }

    /**
     * Is forking supported on this OS?
     *
     * @return True if forking is supported.
     */
    public static boolean canFork() {
        return canFork;
    }

    /**
     * Check if forking is supported on this OS?
     *
     * @return True if forking is supported.
     */
    private static boolean checkCanFork() {
        try {
            JavaVMArguments.of(getPID());
            // If we got here, we can fork, but only allow it when running
            // from a jar.
            final URL url = DFBncDaemon.class.getResource("DFBncDaemon.class");
            return url != null && url.getProtocol().equalsIgnoreCase("jar");
        } catch (final Throwable t) {
            // A few known errors can occur here, all of which mean we can't
            // fork, but also, if this gives ANY error then there's no point
            // trying to fork.
            //
            // IOException - Technically we can fork, but we got an IO Error
            //               reading our own process information, so assume no.
            // UnsupportedOperationException - Akuma doesn't support this OS
            // UnsatisfiedLinkError - Lib C can not be loaded.
            return false;
        }
    }
}

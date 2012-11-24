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
package uk.org.dataforce.dfbncweb;

import uk.org.dataforce.dfbnc.DFBnc;
import uk.org.dataforce.dfbnc.plugins.Plugin;
import uk.org.dataforce.dfbnc.plugins.PluginManager;
import uk.org.dataforce.libs.logger.Logger;

/**
 * DFBnc Web Interface
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class DFBncWeb extends Plugin {
    /** The controlling BNC Instance. */
    private final DFBnc bnc;

    /** The controlling PluginManager Instance. */
    private final PluginManager manager;

    /**
     * Create the DFBncWeb Class.
     */
    public DFBncWeb(final DFBnc bnc, final PluginManager manager) {
        this.bnc = bnc;
        this.manager = manager;
    }

    /** {@inheritDoc} */
    @Override
    public void pluginLoaded() {
        Logger.info("Woo, Plugin!");
        try {
            new DFBncWebService().run(new String[]{"server"});
        } catch (final Exception e) { }
    }

    /** {@inheritDoc} */
    @Override
    public void onShutdown() {
    }

    /** {@inheritDoc} */
    @Override
    public String pluginName() {
        return "DFBncWeb";
    }

    /** {@inheritDoc} */
    @Override
    public String pluginVersion() {
        return "SomeGitRevision";
    }

    /** {@inheritDoc} */
    @Override
    public String pluginDescription() {
        return "JSON Interface to DFBnc";
    }

}

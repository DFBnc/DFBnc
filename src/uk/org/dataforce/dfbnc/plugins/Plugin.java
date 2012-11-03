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
package uk.org.dataforce.dfbnc.plugins;

/**
 * DFBnc Plugin Base Class.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public abstract class Plugin {

    /**
     * Called when the plugin is loaded
     */
    public abstract void pluginLoaded();

    /**
     * Called when the BNC is terminating to allow the plugin to save state.
     *
     * @Deprecated Plugins should hook into the bouncer Event API
     */
    @Deprecated
    public abstract void onShutdown();

    /**
     * Get the plugin name
     *
     * @return Plugin Name
     */
    public abstract String pluginName();

    /**
     * Get the plugin version
     *
     * @return Plugin version
     */
    public abstract String pluginVersion();

    /**
     * Get the plugin description
     *
     * @return Plugin description
     */
    public abstract String pluginDescription();
}

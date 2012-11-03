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

import com.dmdirc.util.SimpleInjector;
import java.util.HashMap;
import java.util.Map;
import uk.org.dataforce.dfbnc.DFBnc;
import uk.org.dataforce.libs.logger.Logger;

/**
 * Class for managing plugins.
 *
 * Currently this just loads them into the current class loader and provides no
 * way to unload, but this may change in future.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class PluginManager {
    /** Loaded Plugins. */
    private final Map<String,Plugin> plugins = new HashMap<String,Plugin>();

    /** Parent DFBnc instance. */
    private final DFBnc bnc;

    /** SimpleInjector. */
    private final SimpleInjector injector;

    /**
     * Create a new PluginManager.
     *
     * @param bnc Parent DFBnc instance.
     */
    public PluginManager(final DFBnc bnc) {
        this.bnc = bnc;
        injector = new SimpleInjector();

        injector.addParameter(this);
        injector.addParameter(bnc);
    }

    /**
     * Load plugin with given class name.
     *
     * @praam classname Class to load
     * @return True if plugin was loaded, else false.
     */
    public boolean loadPlugin(final String classname) {
        final Plugin plugin = doLoadPlugin(classname);

        if (plugin == null) {
            Logger.error("Failed to load plugin: " + classname);
        } else {
            Logger.info("Loaded plugin: " + classname);
            plugins.put(classname, plugin);
        }

        return plugin == null;
    }

    /**
     * Load plugin with given class name.
     *
     * @praam classname Class to load
     * @return Loaded plugin, or null
     */
    private Plugin doLoadPlugin(final String classname) {
        try {
            final Class<?> clazz = Class.forName(classname);
            if (clazz != null) {
                final Object obj = injector.createInstance(clazz);
                if (obj instanceof Plugin) {
                    final Plugin plugin = (Plugin)obj;
                    plugin.pluginLoaded();

                    return plugin;
                }
            }
        } catch (final ClassNotFoundException cnfe) { }

        return null;
    }
}

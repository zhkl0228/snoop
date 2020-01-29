package com.fuzhu8.inspector;

/*-
 * #%L
 * PlugFace :: Core
 * %%
 * Copyright (C) 2017 - 2018 PlugFace
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 * #L%
 */

import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.InspectorPlugin;
import com.fuzhu8.inspector.plugin.Plugin;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

abstract class PluginSource {

    /**
     * Load a list of classes that are either
     * plugins or related code
     *
     * @return a list of class
     * @throws Exception an exception that occurred during loading
     */
    abstract Collection<Class<?>> load() throws Exception;

    final Collection<Plugin> loadPlugins(Instrumentation inst, Appender appender) throws Exception {
        final Collection<Class<?>> pluginClasses = this.load();
        final Collection<Plugin> loaded = new ArrayList<>();

        if (pluginClasses.isEmpty()) {
            return loaded;
        }

        final Iterator<Class<?>> itr = pluginClasses.iterator();
        while (itr.hasNext()) {
            Class<?> pluginClass = itr.next();
            if (pluginClass.isAnnotationPresent(InspectorPlugin.class)) {
                if (AbstractPlugin.class.isAssignableFrom(pluginClass)) {
                    Constructor<?> constructor = pluginClass.getDeclaredConstructor(Instrumentation.class, Appender.class);
                    constructor.setAccessible(true);
                    Plugin plugin = (Plugin) constructor.newInstance(inst, appender);
                    loaded.add(plugin);
                }
                itr.remove();
            }
        }
        return loaded;
    }

}
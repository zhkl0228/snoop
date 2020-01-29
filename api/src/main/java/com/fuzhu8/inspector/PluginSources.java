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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class PluginSources {

    /**
     * Load plugins from JAR files located at the given path
     *
     * @param pluginDirectoryPath the path to the directory where the JAR files are located
     * @return a list of loaded {@link Class} objects, never null
     */
    static PluginSource jarSource(final File pluginDirectoryPath, final ClassLoader parent) {
        return jarSource(pluginDirectoryPath.toURI(), parent);
    }

    /**
     * Load plugins from JAR files located at the given {@link URI}
     *
     * @param pluginUri the {@link URI} to the directory where the JAR files are located
     * @return a list of loaded {@link Class} objects, never null
     */
    private static PluginSource jarSource(final URI pluginUri, final ClassLoader parent) {
        return new PluginSource() {
            @Override
            final Collection<Class<?>> load() throws IOException, ClassNotFoundException {
                final ArrayList<Class<?>> plugins = new ArrayList<>();
                final Path path = Paths.get(pluginUri);
                if (!Files.exists(path)) {
                    throw new IllegalArgumentException("Path " + pluginUri + " does not exist");
                }

                if (!Files.isDirectory(path)) {
                    throw new IllegalArgumentException("Path " + pluginUri + " is not a directory");
                }
                final Map<Path, URL> jarUrls = new HashMap<>();
                for (Path filePath : Files.newDirectoryStream(path)) {
                    if (filePath.getFileName().toString().endsWith(".jar")) {
                        jarUrls.put(filePath, filePath.toUri().toURL());
                    }
                }
                final ClassLoader loader = new URLClassLoader(jarUrls.values().toArray(new URL[]{}), parent);
                for (Path jarPath: jarUrls.keySet()) {
                    final File file = jarPath.toAbsolutePath().toFile();
                    final JarFile jar = new JarFile(file);
                    for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                        final JarEntry entry = entries.nextElement();
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                            continue;
                        }
                        String className = entry.getName().substring(0, entry.getName().length() - 6);
                        className = className.replace('/', '.');
                        Class<?> clazz = Class.forName(className, true, loader);
                        plugins.add(clazz);
                    }
                }
                return plugins;
            }
        };

    }

    static PluginSource jarSource(final File jarFile, final ClassNameFilter filter) {
        return new PluginSource() {
            @Override
            final Collection<Class<?>> load() throws IOException, ClassNotFoundException {
                final ArrayList<Class<?>> plugins = new ArrayList<>();
                final ClassLoader loader = new URLClassLoader(new URL[] { jarFile.toURI().toURL() });
                final JarFile jar = new JarFile(jarFile);
                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                    final JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    String className = entry.getName().substring(0, entry.getName().length() - 6);
                    className = className.replace('/', '.');
                    if (filter.accept(className)) {
                        Class<?> clazz = Class.forName(className, true, loader);
                        plugins.add(clazz);
                    }
                }
                return plugins;
            }
        };

    }

}
package com.fuzhu8.inspector;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.jar.JarFile;

public class InspectorAgent {

    public static void premain(String args, Instrumentation instrumentation) {
        new InspectorAgent().premain(instrumentation);
    }

    private void premain(Instrumentation instrumentation) {
        try {
            CodeSource codeSource = InspectorAgent.class.getProtectionDomain().getCodeSource();
            final Path path = Paths.get(codeSource.getLocation().toURI());
            final File jarFile = path.toAbsolutePath().toFile();
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(jarFile));

            ClassLoader classLoader = InspectorAgent.class.getClassLoader();
            Class<?> cPluginInitializer = classLoader.loadClass("com.fuzhu8.inspector.PluginInitializer");

            Constructor<?> constructor = cPluginInitializer.getDeclaredConstructor(Instrumentation.class);
            constructor.setAccessible(true);
            Object initializer = constructor.newInstance(instrumentation);
            Method initialize = cPluginInitializer.getDeclaredMethod("initialize", File.class);
            initialize.setAccessible(true);
            initialize.invoke(initializer, jarFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

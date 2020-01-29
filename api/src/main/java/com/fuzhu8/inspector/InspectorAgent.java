package com.fuzhu8.inspector;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Properties;
import java.util.jar.JarFile;

public class InspectorAgent {

    public static void premain(String args, Instrumentation instrumentation) {
        turnOffSecurity();
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

    private static void turnOffSecurity() {
        /*
         * Test if we're inside an applet. We should be inside
         * an applet if the System property ("package.restrict.access.sun")
         * is not null and is set to true.
         */

        boolean restricted = System.getProperty("package.restrict.access.sun") != null;

        /*
         * If we're in an applet, we need to change the System properties so
         * as to avoid class restrictions. We go through the current properties
         * and remove anything related to package restriction.
         */
        if ( restricted ) {
            Properties newProps = new Properties();

            Properties sysProps = System.getProperties();

            for(String prop : sysProps.stringPropertyNames()) {
                if ( prop != null && ! prop.startsWith("package.restrict.") ) {
                    newProps.setProperty(prop,sysProps.getProperty(prop));
                }
            }

            System.setProperties(newProps);
        }

        /*
         * Should be the final nail in (your) coffin.
         */
        System.setSecurityManager(null);
    }

}

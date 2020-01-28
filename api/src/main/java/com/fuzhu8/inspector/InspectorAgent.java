package com.fuzhu8.inspector;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public class InspectorAgent {

    public static void premain(String args, Instrumentation instrumentation) {
        new InspectorAgent().premain(instrumentation);
    }

    private void premain(Instrumentation instrumentation) {
        try {
            CodeSource codeSource = InspectorAgent.class.getProtectionDomain().getCodeSource();
            final Path path = Paths.get(codeSource.getLocation().toURI());
            final File jarFile = path.toAbsolutePath().toFile();
            new PluginInitializer(instrumentation).initialize(jarFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

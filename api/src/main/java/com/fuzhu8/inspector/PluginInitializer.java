package com.fuzhu8.inspector;

import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.plugin.PluginClassFileTransformer;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class PluginInitializer implements Appender, ClassNameFilter {

    private final Instrumentation instrumentation;

    PluginInitializer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @SuppressWarnings("unused")
    final void initialize(File pluginJar) {
        PluginSource pluginSource = PluginSources.jarSource(pluginJar, this);
        initializePluginSource(pluginSource, instrumentation, this);
    }

    static void initializePluginSource(PluginSource pluginSource, Instrumentation instrumentation, Appender appender) {
        try {
            Collection<Plugin> plugins = pluginSource.loadPlugins(instrumentation, appender);
            for (Plugin plugin : plugins) {
                instrumentation.addTransformer(new PluginClassFileTransformer(appender, plugin), true);
                appender.out_println("Discover plugin: " + plugin);
            }

            List<Class<?>> list = new ArrayList<>();
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                for (Plugin plugin : plugins) {
                    if (plugin.selectClassTransformer(loaded.getName().replace('.', '/')) != null) {
                        list.add(loaded);
                    }
                }
            }
            if (!list.isEmpty()) {
                instrumentation.retransformClasses(list.toArray(new Class<?>[0]));
            }
        } catch (Exception e) {
            appender.printStackTrace(e);
        }
    }

    @Override
    public boolean accept(String className) {
        boolean isApi = className.startsWith("javassist.") ||
                className.startsWith("com.fuzhu8.inspector.") ||
                className.startsWith("de.robv.android.xposed.");
        return !isApi;
    }

    @Override
    public void out_print(Object msg) {
        System.out.print(msg);
    }

    @Override
    public void out_println(Object msg) {
        System.out.println(msg);
    }

    @Override
    public void err_print(Object msg) {
        System.err.print(msg);
    }

    @Override
    public void err_println(Object msg) {
        System.err.println(msg);
    }

    @Override
    public void printStackTrace(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public boolean isDebug() {
        return false;
    }

}

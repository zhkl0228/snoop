package com.fuzhu8.inspector;

import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.plugin.PluginClassFileTransformer;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collection;

@SuppressWarnings("unused")
class PluginInitializer implements Appender, ClassNameFilter {

    private final Instrumentation instrumentation;

    PluginInitializer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    final void initialize(File pluginJar) {
        try {
            PluginSource pluginSource = PluginSources.jarSource(pluginJar, this);

            Collection<Plugin> plugins = pluginSource.loadPlugins(this);
            for (Plugin plugin : plugins) {
                instrumentation.addTransformer(new PluginClassFileTransformer(this, plugin));
                this.out_println("Discover plugin: " + plugin);
            }
        } catch (Exception e) {
            this.printStackTrace(e);
        }
    }

    @Override
    public boolean accept(String className) {
        boolean isApi = className.startsWith("javassist.") ||
                className.startsWith("com.fuzhu8.") ||
                className.startsWith("de.robv.android.");
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

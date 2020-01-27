package com.fuzhu8.inspector;

import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.plugin.PluginClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.util.Collection;

public class InspectorAgent implements Appender, ClassNameFilter {

    public static void premain(String args, Instrumentation instrumentation) {
        new InspectorAgent().premain(instrumentation);
    }

    @Override
    public boolean accept(String className) {
        return !className.startsWith("javassist.");
    }

    private void premain(Instrumentation instrumentation) {
        try {
            CodeSource codeSource = InspectorAgent.class.getProtectionDomain().getCodeSource();
            PluginSource pluginSource = PluginSources.singleJarSource(codeSource.getLocation().toURI(), this);

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

package com.fuzhu8.inspector.plugin;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPlugin implements Plugin {

    protected final Instrumentation inst;
    protected final Appender appender;

    public AbstractPlugin(Instrumentation inst, Appender appender) {
        this.inst = inst;
        this.appender = appender;

        initialize();
    }

    protected boolean debug;

    @Override
    public boolean isDebug() {
        return debug;
    }

    protected abstract void initialize();

    @Override
    public ClassTransformer selectClassTransformer(String className) {
        return transformerMap.get(className);
    }

    private final Map<String, ClassTransformer> transformerMap = new HashMap<>();

    protected final void registerClassTransformer(String className, ClassTransformer transformer) {
        transformerMap.put(className, transformer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}

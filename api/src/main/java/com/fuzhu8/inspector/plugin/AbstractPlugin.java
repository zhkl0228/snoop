package com.fuzhu8.inspector.plugin;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

public abstract class AbstractPlugin implements Plugin {

    protected final Appender appender;

    public AbstractPlugin(Appender appender) {
        this.appender = appender;
    }

    @Override
    public byte[] onTransform(ClassLoader loader, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        if (appender.isDebug()) {
            appender.out_println("onTransform loader=" + loader + ", class=" + cc.getName());
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}

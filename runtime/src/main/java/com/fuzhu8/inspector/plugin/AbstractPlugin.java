package com.fuzhu8.inspector.plugin;

import com.fuzhu8.inspector.Inspector;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

public abstract class AbstractPlugin implements Plugin {

    protected final Inspector inspector;

    public AbstractPlugin(Inspector inspector) {
        this.inspector = inspector;
    }

    @Override
    public byte[] onTransform(ClassLoader loader, CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        if (inspector.isDebug()) {
            inspector.println("onTransform loader=" + loader + ", class=" + clazz.getName());
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}

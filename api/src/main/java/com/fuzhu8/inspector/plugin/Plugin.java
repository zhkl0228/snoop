package com.fuzhu8.inspector.plugin;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

public interface Plugin {

    byte[] onTransform(ClassLoader loader, CtClass cc) throws NotFoundException, CannotCompileException, IOException;

}

package com.fuzhu8.inspector.plugin;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

public interface ClassTransformer {

    byte[] transform(CtClass cc) throws NotFoundException, CannotCompileException, IOException;

}

package de.robv.android.xposed;

import javassist.CannotCompileException;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;

public interface HookBuilder {

    HookBuilder hook(CtMethod method, XC_MethodHook callback) throws NotFoundException, CannotCompileException;

    HookBuilder hook(CtConstructor constructor, XC_MethodHook callback) throws NotFoundException, CannotCompileException;

    byte[] build() throws CannotCompileException, IOException;

}

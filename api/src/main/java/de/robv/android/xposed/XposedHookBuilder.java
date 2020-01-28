package de.robv.android.xposed;

import com.fuzhu8.inspector.plugin.Appender;
import javassist.*;

import java.io.IOException;

public class XposedHookBuilder implements HookBuilder {

    public static HookBuilder createBuilder(CtClass ctClass, Appender appender) {
        return new XposedHookBuilder(ctClass, appender);
    }

    private final CtClass cc;
    private final Appender appender;

    private XposedHookBuilder(CtClass ctClass, Appender appender) {
        this.cc = ctClass;
        this.appender = appender;
    }

    @Override
    public HookBuilder hook(CtMethod method, XC_MethodHook callback) throws NotFoundException, CannotCompileException {
        if (method.getDeclaringClass() != cc) {
            throw new IllegalStateException("declaringClass=" + method.getDeclaringClass() + ", cc=" + cc);
        }

        XposedBridge.hookMethod(cc, method, callback);
        if (appender != null) {
            appender.out_println(">-----------------------------------------------------------------------------<");
            appender.out_println("[HOOK]: " + method.getLongName());
            appender.out_println("^-----------------------------------------------------------------------------^");
        }
        return this;
    }

    @Override
    public HookBuilder hook(CtConstructor constructor, XC_MethodHook callback) throws NotFoundException, CannotCompileException {
        if (constructor.getDeclaringClass() != cc) {
            throw new IllegalStateException("declaringClass=" + constructor.getDeclaringClass() + ", cc=" + cc);
        }

        XposedBridge.hookConstructor(cc, constructor, callback);
        if (appender != null) {
            appender.out_println(">-----------------------------------------------------------------------------<");
            appender.out_println("[HOOK]: " + constructor.getLongName());
            appender.out_println("^-----------------------------------------------------------------------------^");
        }
        return this;
    }

    @Override
    public HookBuilder hookClassInitializer(XC_MethodHook callback) throws NotFoundException, CannotCompileException {
        CtConstructor classInitializer = cc.getClassInitializer();
        if (classInitializer == null) {
            throw new NotFoundException("Not found class initializer: " + cc.getName());
        }
        XposedBridge.hookClassInitializer(cc, classInitializer, callback);
        if (appender != null) {
            appender.out_println(">-----------------------------------------------------------------------------<");
            appender.out_println("[HOOK]: " + classInitializer.getLongName());
            appender.out_println("^-----------------------------------------------------------------------------^");
        }
        return this;
    }

    @Override
    public byte[] build() throws CannotCompileException, IOException {
        byte[] newByteCode = cc.toBytecode();
        if(appender != null && appender.isDebug()) {
            cc.writeFile(System.getProperty("user.home") + "/.snoop/tmp");
        }
        return newByteCode;
    }

}

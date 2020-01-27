package de.robv.android.xposed;

import com.fuzhu8.inspector.plugin.Appender;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

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
        appender.out_println("Hook method: " + method);
        return this;
    }

    @Override
    public byte[] build() throws CannotCompileException, IOException {
        byte[] newByteCode = cc.toBytecode();
        if(appender.isDebug()) {
            cc.writeFile(System.getProperty("user.home") + "/.snoop/tmp");
        }
        return newByteCode;
    }

}

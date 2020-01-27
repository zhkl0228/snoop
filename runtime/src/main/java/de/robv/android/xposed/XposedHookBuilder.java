package de.robv.android.xposed;

import com.fuzhu8.inspector.Inspector;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;

public class XposedHookBuilder implements HookBuilder {

    public static HookBuilder createBuilder(CtClass ctClass, Inspector inspector) {
        return new XposedHookBuilder(ctClass, inspector);
    }

    private final CtClass cc;
    private final Inspector inspector;

    private XposedHookBuilder(CtClass ctClass, Inspector inspector) {
        this.cc = ctClass;
        this.inspector = inspector;
    }

    @Override
    public HookBuilder hook(CtMethod method, XC_MethodHook callback) throws NotFoundException, CannotCompileException {
        if (method.getDeclaringClass() != cc) {
            throw new IllegalStateException("declaringClass=" + method.getDeclaringClass() + ", cc=" + cc);
        }

        XposedBridge.hookMethod(cc, method, callback);
        inspector.println("Hook method: " + method);
        return this;
    }

    @Override
    public byte[] build() throws CannotCompileException, IOException {
        byte[] newByteCode = cc.toBytecode();
        if(inspector.isDebug()) {
            cc.writeFile(System.getProperty("user.home") + "/.snoop/tmp");
        }
        return newByteCode;
    }

}

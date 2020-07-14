package com.aspect.snoop.plugin;

import com.aspect.snoop.plugin.jeb.JebNetPostHandler;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.ClassTransformer;
import de.robv.android.xposed.XposedHookBuilder;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * disable check update
 */
public class JebNetTransformer implements ClassTransformer {

    private final Appender appender;

    JebNetTransformer(Appender appender) {
        this.appender = appender;
    }

    @Override
    public String getClassName() {
        return "com/pnfsoftware/jeb/client/JebNet";
    }

    @Override
    public byte[] transform(Class<?> classBeingRedefined, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        CtMethod[] methods = cc.getDeclaredMethods("post");
        for (CtMethod method : methods) {
            if (method.getParameterTypes().length == 4) {
                return XposedHookBuilder.createBuilder(cc, appender).hook(method, new JebNetPostHandler(appender)).build();
            }
        }
        return null;
    }

}

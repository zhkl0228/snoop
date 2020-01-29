package com.aspect.snoop.plugin;

import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.ClassTransformer;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHookBuilder;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.Arrays;

public class RuntimeTransformer extends XC_MethodHook implements ClassTransformer {

    private final Appender appender;

    RuntimeTransformer(Appender appender) {
        this.appender = appender;
    }

    @Override
    public byte[] transform(Class<?> classBeingRedefined, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        for (CtMethod method : cc.getDeclaredMethods("exec")) {
            CtClass[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && "java.lang.String[]".equals(parameterTypes[0].getName())) {
                return XposedHookBuilder.createBuilder(cc, appender).hook(method, this).build();
            }
        }
        return null;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        String[] cmdArray = (String[]) param.args[0];
        appender.out_println("Runtime.exec: " + Arrays.toString(cmdArray));
    }
}

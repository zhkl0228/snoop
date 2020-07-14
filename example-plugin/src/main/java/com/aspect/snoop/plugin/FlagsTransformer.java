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

public class FlagsTransformer extends XC_MethodHook implements ClassTransformer {

    private final Appender appender;

    FlagsTransformer(Appender appender) {
        this.appender = appender;
    }

    @Override
    public String getClassName() {
        return "com/pnfsoftware/jeb/util/base/Flags";
    }

    @Override
    public byte[] transform(Class<?> classBeingRedefined, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        CtMethod method = cc.getDeclaredMethod("has");
        return XposedHookBuilder.createBuilder(cc, appender).hook(method, this).build();
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
        int value = (Integer) param.args[0];
        if (value == 0x10) {
            param.setResult(Boolean.TRUE); // disable require internet
        }
        appender.out_println("Flags.has(0x" + Integer.toHexString(value) + ") => " + param.getResult());
    }
}

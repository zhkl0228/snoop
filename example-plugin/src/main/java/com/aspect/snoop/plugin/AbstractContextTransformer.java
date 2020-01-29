package com.aspect.snoop.plugin;

import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.ClassTransformer;
import de.robv.android.xposed.XC_ClassInitializerHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHookBuilder;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

public class AbstractContextTransformer extends XC_ClassInitializerHook implements ClassTransformer {

    private final Appender appender;

    AbstractContextTransformer(Appender appender) {
        this.appender = appender;
    }

    @Override
    public byte[] transform(CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        return XposedHookBuilder.createBuilder(cc, appender).hookClassInitializer(this).build();
    }

    @Override
    protected void afterHookedClassInitializer(ClassInitializerHookParam param) throws Throwable {
        super.afterHookedClassInitializer(param);

        String app_ver = String.valueOf(XposedHelpers.getStaticObjectField(param.thisClass, "app_ver"));
        appender.out_println("Jeb version found: " + app_ver);
    }
}

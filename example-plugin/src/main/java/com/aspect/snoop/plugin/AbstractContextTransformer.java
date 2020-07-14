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
    public String getClassName() {
        return "com/pnfsoftware/jeb/client/AbstractContext";
    }

    @Override
    public byte[] transform(Class<?> classBeingRedefined, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        if (classBeingRedefined != null) {
            findAppVer(classBeingRedefined);
        }
        return XposedHookBuilder.createBuilder(cc, appender).hookClassInitializer(this).build();
    }

    @Override
    protected void afterHookedClassInitializer(Class<?> thisClass) throws Throwable {
        super.afterHookedClassInitializer(thisClass);

        findAppVer(thisClass);
    }

    private void findAppVer(Class<?> thisClass) {
        String app_ver = String.valueOf(XposedHelpers.getStaticObjectField(thisClass, "app_ver"));
        appender.out_println("Jeb version found: " + app_ver);
    }
}

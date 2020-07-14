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

public class LicensingTransformer extends XC_ClassInitializerHook implements ClassTransformer {

    private static final int FLAG_AIR_GAP = 0x8;

    private final Appender appender;

    public LicensingTransformer(Appender appender) {
        this.appender = appender;
    }

    @Override
    public String getClassName() {
        return "com/pnfsoftware/jeb/client/Licensing";
    }

    @Override
    public byte[] transform(Class<?> classBeingRedefined, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        if (classBeingRedefined != null) {
            fakeLicense(classBeingRedefined);
        }

        return XposedHookBuilder.createBuilder(cc, appender).hookClassInitializer(this).build();
    }

    @Override
    protected void afterHookedClassInitializer(Class<?> thisClass) throws Throwable {
        super.afterHookedClassInitializer(thisClass);

        fakeLicense(thisClass);
    }

    private void fakeLicense(Class<?> thisClass) {
        int build_type = XposedHelpers.getStaticIntField(thisClass, "build_type");
        build_type |= FLAG_AIR_GAP;
        XposedHelpers.setStaticIntField(thisClass, "build_type", build_type);

        int license_validity = XposedHelpers.getStaticIntField(thisClass, "license_validity");
        license_validity += (365 * 10); // 10 years
        XposedHelpers.setStaticIntField(thisClass, "license_validity", license_validity);
    }
}

package com.aspect.snoop.plugin;

import com.aspect.snoop.plugin.jeb.JebNetPostHandler;
import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.InspectorPlugin;
import com.fuzhu8.inspector.plugin.Plugin;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHookBuilder;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;

@InspectorPlugin
@SuppressWarnings("unused")
public class JebPlugin extends AbstractPlugin implements Plugin {

    public JebPlugin(Appender appender) {
        super(appender);

        System.out.println("Initialize crack jeb plugin");
    }

    @Override
    public byte[] onTransform(ClassLoader loader, CtClass cc) throws NotFoundException, CannotCompileException, IOException {
        if ("com.pnfsoftware.jeb.client.JebNet".equals(cc.getName())) {
            CtMethod[] methods = cc.getDeclaredMethods("post");
            for (CtMethod method : methods) {
                if (method.getParameterTypes().length == 4) {
                    appender.out_println("Hook method=" + method);
                    return XposedBridge.hookMethod(method, new JebNetPostHandler(appender)); // disable update check
                }
            }
        } else if ("com.pnfsoftware.jeb.util.base.Flags".equals(cc.getName())) {
            CtMethod method = cc.getDeclaredMethod("has");
            return XposedHookBuilder.createBuilder(cc, appender).hook(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    int value = (Integer) param.args[0];
                    if (value == 0x10) {
                        param.setResult(Boolean.TRUE); // disable require internet
                    }
                    appender.out_println("Flags.has(0x" + Integer.toHexString(value) + ") => " + param.getResult());
                }
            }).build();
        } else if ("com.pnfsoftware.jeb.client.AbstractContext".equals(cc.getName())) {
            return XposedHookBuilder.createBuilder(cc, appender).hookClassInitializer(new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    String app_ver = String.valueOf(XposedHelpers.getStaticObjectField((Class<?>) param.thisObject, "app_ver"));
                    appender.out_println("Jeb version found: " + app_ver);
                }
            }).build();
        } else if ("com.pnfsoftware.jeb.client.Licensing".equals(cc.getName())) {
            return XposedHookBuilder.createBuilder(cc, appender).hookClassInitializer(new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    Class<?> clazz = (Class<?>) param.thisObject;
                    int build_type = XposedHelpers.getStaticIntField(clazz, "build_type");
                    build_type |= FLAG_AIRGAP;
                    XposedHelpers.setStaticIntField(clazz, "build_type", build_type);

                    int license_validity = XposedHelpers.getStaticIntField(clazz, "license_validity");
                    license_validity += (365 * 10); // 20 years
                    XposedHelpers.setStaticIntField(clazz, "license_validity", license_validity);
                }
            }).build();
        }

        return super.onTransform(loader, cc);
    }

    private static final int FLAG_AIRGAP = 8;

}

package com.aspect.snoop.plugin;

import com.aspect.snoop.plugin.jeb.JebNetPostHandler;
import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.InspectorPlugin;
import com.fuzhu8.inspector.plugin.Plugin;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
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
    public byte[] onTransform(ClassLoader loader, CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        if ("com.pnfsoftware.jeb.client.JebNet".equals(clazz.getName())) {
            CtMethod[] methods = clazz.getDeclaredMethods("post");
            for (CtMethod method : methods) {
                if (method.getParameterTypes().length == 4) {
                    appender.out_println("Hook method=" + method);
                    return XposedBridge.hookMethod(method, new JebNetPostHandler(appender)); // disable update check
                }
            }
        } else if ("com.pnfsoftware.jeb.util.base.Flags".equals(clazz.getName())) {
            CtMethod method = clazz.getDeclaredMethod("has");
            return XposedHookBuilder.createBuilder(clazz, appender).hook(method, new XC_MethodHook() {
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
        }

        return super.onTransform(loader, clazz);
    }
}

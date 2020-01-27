package com.aspect.snoop.plugin;

import com.aspect.snoop.plugin.jeb.JebNetPostHandler;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Plugin;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHookBuilder;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;

public class ExamplePlugin extends AbstractPlugin implements Plugin {

    public ExamplePlugin(Inspector inspector) {
        super(inspector);

        System.out.println("Initialize example jeb plugin");
    }

    @Override
    public byte[] onTransform(ClassLoader loader, CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        if ("com.pnfsoftware.jeb.client.JebNet".equals(clazz.getName())) {
            CtMethod[] methods = clazz.getDeclaredMethods("post");
            for (CtMethod method : methods) {
                if (method.getParameterTypes().length == 4) {
                    inspector.println("Hook method=" + method);
                    return XposedBridge.hookMethod(method, new JebNetPostHandler(inspector));
                }
            }
        } else if ("com.pnfsoftware.jeb.util.base.Flags".equals(clazz.getName())) {
            CtMethod method = clazz.getDeclaredMethod("has");
            return XposedHookBuilder.createBuilder(clazz, inspector).hook(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    int value = (Integer) param.args[0];
                    if (value == 0x10) {
                        param.setResult(Boolean.TRUE);
                    }
                    inspector.println("Flags.has(0x" + Integer.toHexString(value) + ") => " + param.getResult());
                }
            }).build();
        }

        return super.onTransform(loader, clazz);
    }
}

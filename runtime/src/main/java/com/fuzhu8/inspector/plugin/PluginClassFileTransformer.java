package com.fuzhu8.inspector.plugin;

import com.fuzhu8.inspector.Inspector;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class PluginClassFileTransformer implements ClassFileTransformer {

    private final Inspector inspector;
    private final Plugin plugin;

    public PluginClassFileTransformer(Inspector inspector, Plugin plugin) {
        this.inspector = inspector;
        this.plugin = plugin;
    }

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {
            if (classBeingRedefined != null) {
                return classfileBuffer;
            }
            ClassPool cp = ClassPool.getDefault();
            CtClass clazz = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
            byte[] classData = plugin.onTransform(loader, clazz);
            if (classData != null) {
                return classData;
            }
        } catch (Exception e) {
            inspector.inspect(classfileBuffer, "transform failed: className=" + className + ", classBeingRedefined=" + classBeingRedefined + ", loader=" + loader);
            inspector.println(e);
        }
        return classfileBuffer;
    }

}

package com.fuzhu8.inspector.plugin;

import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class PluginClassFileTransformer implements ClassFileTransformer {

    private final Appender appender;
    private final Plugin plugin;

    public PluginClassFileTransformer(Appender appender, Plugin plugin) {
        this.appender = appender;
        this.plugin = plugin;
    }

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {
            if (classBeingRedefined != null || className == null) {
                return classfileBuffer;
            }

            ClassTransformer classTransformer = plugin.selectClassTransformer(className);
            if (classTransformer != null) {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
                byte[] classData = classTransformer.transform(cc);
                if (classData != null) {
                    return classData;
                }
            }
        } catch (Throwable e) {
            appender.out_println("transform failed: className=" + className + ", loader=" + loader);
            appender.printStackTrace(e);
        }
        return classfileBuffer;
    }

}

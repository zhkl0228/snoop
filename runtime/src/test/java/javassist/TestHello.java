package javassist;

import de.robv.android.xposed.*;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;

/**
 * @author zhkl0228
 *
 */
public class TestHello extends TestCase {

	private static class MyClassLoader extends ClassLoader {

		public MyClassLoader(ClassLoader parent) {
			super(parent);
		}
	}

	public void testSayHello() throws Exception {
		ClassPool cp = ClassPool.getDefault();

		CtClass cc = cp.get("javassist.Hello");

		CtMethod cm = cc.getDeclaredMethod("say");

		byte[] classData = XposedHookBuilder.createBuilder(cc, null).hook(cm, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				System.out.println("beforeHookedMethod method=" + param.method + ", thisObject=" + param.thisObject);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				System.out.println("afterHookedMethod method=" + param.method + ", thisObject=" + param.thisObject);
				param.setResult("Test");
			}
		}).hook(cc.getDeclaredConstructor(new CtClass[0]), new XC_ConstructorHook() {
			@Override
			protected void beforeHookedConstructor(Constructor<?> constructor, Class<?> thisClass) throws Throwable {
				super.beforeHookedConstructor(constructor, thisClass);
				System.out.println("beforeHookedConstructor constructor=" + constructor + ", thisClass=" + thisClass);
			}

			@Override
			protected void afterHookedConstructor(Constructor<?> constructor, Object thisObject) throws Throwable {
				super.afterHookedConstructor(constructor, thisObject);
				System.out.println("afterHookedConstructor constructor=" + constructor + ", thisObject=" + thisObject);
			}
		}).hookClassInitializer(new XC_ClassInitializerHook() {
			@Override
			protected void beforeHookedClassInitializer(Class<?> thisClass) throws Throwable {
				super.beforeHookedClassInitializer(thisClass);
				System.out.println("beforeHookedClassInitializer thisClass=" + thisClass);
			}

			@Override
			protected void afterHookedClassInitializer(Class<?> thisClass) throws Throwable {
				super.afterHookedClassInitializer(thisClass);
				System.out.println("afterHookedClassInitializer thisClass=" + thisClass);
			}
		}).build();
		assertNotNull(classData);
		cc.defrost();

		CtClass newClass = cp.makeClass(new ByteArrayInputStream(classData));
		Class<?> c = newClass.toClass(new MyClassLoader(cp.getClassLoader()), null);

		Object obj = c.newInstance();
		System.out.println(XposedHelpers.callMethod(obj, "say"));
	}

}

package javassist;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHookBuilder;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

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
				System.out.println("beforeHookedMethod method=" + param.method + ", this=" + param.thisObject);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				System.out.println("afterHookedMethod method=" + param.method + ", this=" + param.thisObject);
				param.setResult("Test");
			}
		}).hook(cc.getDeclaredConstructor(new CtClass[0]), new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				System.out.println("beforeHookedMethod method=" + param.method + ", this=" + param.thisObject);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				System.out.println("afterHookedMethod method=" + param.method + ", this=" + param.thisObject);
			}
		}).hookClassInitializer(new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				System.out.println("beforeHookedMethod method=" + param.method + ", this=" + param.thisObject);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				System.out.println("afterHookedMethod method=" + param.method + ", this=" + param.thisObject);
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

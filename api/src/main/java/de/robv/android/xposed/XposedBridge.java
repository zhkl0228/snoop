package de.robv.android.xposed;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XCMethodPointer;
import de.robv.android.xposed.callbacks.XCallback;
import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class XposedBridge {

	private static final Object[] EMPTY_ARRAY = new Object[0];

	/**
	 * Writes a message to the Xposed error log.
	 *
	 * <p>DON'T FLOOD THE LOG!!! This is only meant for error logging.
	 * If you want to write information/debug messages, use logcat.
	 *
	 * @param text The log message.
	 */
	public synchronized static void log(String text) {
		System.err.println(text);
	}

	/**
	 * Logs a stack trace to the Xposed error log.
	 *
	 * <p>DON'T FLOOD THE LOG!!! This is only meant for error logging.
	 * If you want to write information/debug messages, use logcat.
	 *
	 * @param t The Throwable object for the stack trace.
	 */
	public synchronized static void log(Throwable t) {
		t.printStackTrace();
	}
	
    private static final int SYNTHETIC = 0x00001000;
	
	public static boolean isSynthetic(int mod) {
		return (mod & SYNTHETIC) != 0;
	}

	/**
	 * Hook any method with the specified callback
	 *
	 * @param hookMethod The method to be hooked
	 */
	public static void hookMethod(Member hookMethod, XCallback callback) {
		if (hookMethod.getDeclaringClass().isInterface()) {
			throw new IllegalArgumentException("Cannot hook interfaces: " + hookMethod);
		} else if (Modifier.isAbstract(hookMethod.getModifiers())) {
			throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod);
		} else if(Modifier.isNative(hookMethod.getModifiers())) {
			throw new IllegalArgumentException("Cannot hook native methods: " + hookMethod);
		} else if(isSynthetic(hookMethod.getModifiers())) {
			throw new IllegalArgumentException("Cannot hook synthetic methods: " + hookMethod);
		}

		hookMethodNative(hookMethod, callback);
	}
	
	private static Instrumentation instrumentation;

	public static void initialize(Instrumentation instrumentation) {
		XposedBridge.instrumentation = instrumentation;
	}

	@SuppressWarnings("unused")
	public static boolean isBridgeInvoked(Method method) {
		boolean isBridgeInvoked = false;
		boolean callFromBody = false;
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		for(StackTraceElement element : elements) {
			if("java.lang.Thread".equals(element.getClassName()) && "getStackTrace".equals(element.getMethodName())) {
				continue;
			}
			
			if("de.robv.android.xposed.XposedBridge".equals(element.getClassName()) && "isBridgeInvoked".equals(element.getMethodName())) {
				isBridgeInvoked = true;
				continue;
			}
			
			if(isBridgeInvoked && !callFromBody && element.getClassName().equals(method.getDeclaringClass().getName()) && method.getName().equals(element.getMethodName())) {
				callFromBody = true;
				continue;
			}
			// System.err.println("className=" + method.getDeclaringClass().getName() + ", methodName=" + method.getName());
			
			if(callFromBody && element.getClassName().contains("reflect") && element.getMethodName().contains("invoke")) { // invokeOriginalMethodNative: method.invoke
				continue;
			}
			
			if(callFromBody && "de.robv.android.xposed.XposedBridge".equals(element.getClassName()) && "invokeOriginalMethodNative".equals(element.getMethodName())) {
				return true;
			}
			
			break;
		}
		return false;
	}

	/**
	 * Intercept every call to the specified method and call a handler function instead.
	 * @param method The method to intercept
	 */
	private synchronized static void hookMethodNative(Member method, XCallback callback) {
		if(instrumentation == null) {
			throw new IllegalStateException("instrumentation is null");
		}
		if(!instrumentation.isRedefineClassesSupported()) {
			throw new UnsupportedOperationException("Redefine Classes not Supported");
		}
		Class<?> declaringClass = method.getDeclaringClass();
		if(!instrumentation.isModifiableClass(declaringClass)) {
			throw new UnsupportedOperationException("Can not modify the class: " + declaringClass.getCanonicalName());
		}
		if (!(method instanceof Method) && !(method instanceof Constructor)) {
			throw new IllegalArgumentException("Only method or constructor can be hooked: " + method);
		}

		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.get(declaringClass.getName());

			if (method instanceof Method) {
				String methodName = method.getName();

				// get the parameters in order so we can get the method to instrument
				Class<?>[] parameterTypes = ((Method) method).getParameterTypes();

				CtClass[] classes = new CtClass[parameterTypes.length];
				for(int i=0; i < parameterTypes.length; i++) {
					classes[i] = cp.get(parameterTypes[i].getName());
				}

				CtMethod cm = cc.getDeclaredMethod(methodName, classes);

				try {
					byte[] newByteCode = hookMethod(cm, (XC_MethodHook) callback);
					if (newByteCode != null) {
						ClassDefinition definition = new ClassDefinition(declaringClass, newByteCode);
						instrumentation.redefineClasses(definition);
					}
				} catch (VerifyError error) {
					System.err.println("hook method failed: " + method);
					error.printStackTrace();
					cc.writeFile(System.getProperty("user.home") + "/.snoop/tmp");
				}
			} else {
				// get the parameters in order so we can get the method to instrument
				Class<?>[] parameterTypes = ((Constructor<?>) method).getParameterTypes();

				CtClass[] classes = new CtClass[parameterTypes.length];
				for(int i=0; i < parameterTypes.length; i++) {
					classes[i] = cp.get(parameterTypes[i].getName());
				}

				CtConstructor constructor = cc.getDeclaredConstructor(classes);

				try {
					byte[] newByteCode = XposedHookBuilder.createBuilder(cc, null).hook(constructor, (XC_ConstructorHook) callback).build();
					if (newByteCode != null) {
						ClassDefinition definition = new ClassDefinition(declaringClass, newByteCode);
						instrumentation.redefineClasses(definition);
					}
				} catch (VerifyError error) {
					System.err.println("hook constructor failed: " + method);
					error.printStackTrace();
					cc.writeFile(System.getProperty("user.home") + "/.snoop/tmp");
				}
			}
		} catch (Exception e) {
			log(e);
		}
	}

	/**
	 * Intercept every call to the specified method and call a handler function instead.
	 * @param method The method to intercept
	 */
	private synchronized static byte[] hookMethod(CtMethod method, XC_MethodHook callback) {
		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass declaringClass = method.getDeclaringClass();
			CtClass returnType = method.getReturnType();

			// unfreeze the class so we can modify it
			declaringClass.defrost();

			String thiz;
			if(Modifier.isStatic(method.getModifiers())) {
				thiz = declaringClass.getName() + ".class";
			} else {
				thiz = "$0";
			}

			final int methodId = Objects.hashCode(method.getLongName() + method.getReturnType().getName());
			String retExpr;
			if(returnType == CtClass.voidType) {
				retExpr = "de.robv.android.xposed.XposedBridge.handleHookedMethod(" + methodId + ", method, " + thiz + ", $args);return;\n";
			} else {
				retExpr = "Object ret = de.robv.android.xposed.XposedBridge.handleHookedMethod(" + methodId + ", method, " + thiz + ", $args);";

				if(returnType == CtClass.booleanType) {
					retExpr += "return ((Boolean) ret).booleanValue();\n";
				} else if(returnType == CtClass.byteType) {
					retExpr += "return ((Byte) ret).byteValue();\n";
				} else if(returnType == CtClass.intType) {
					retExpr += "return ((Integer) ret).intValue();\n";
				} else if(returnType == CtClass.charType) {
					retExpr += "return ((Character) ret).charValue();\n";
				} else if(returnType == CtClass.shortType) {
					retExpr += "return ((Short) ret).shortValue();\n";
				} else if(returnType == CtClass.longType) {
					retExpr += "return ((Long) ret).longValue();\n";
				} else if(returnType == CtClass.floatType) {
					retExpr += "return ((Float) ret).floatValue();\n";
				} else if(returnType == CtClass.doubleType) {
					retExpr += "return ((Double) ret).doubleValue();\n";
				} else if(returnType != cp.get("java.lang.Object")) {
					retExpr += "return (" + returnType.getName() + ") ret;\n";
				} else {
					retExpr += "return ret;\n";
				}
			}

			CopyOnWriteSortedSet<XCallback> callbacks;
			synchronized (ctHookedMethodCallbacks) {
				callbacks = ctHookedMethodCallbacks.get(methodId);
				if (callbacks == null) {
					callbacks = new CopyOnWriteSortedSet<>();
					ctHookedMethodCallbacks.put(methodId, callbacks);
				}
			}
			callbacks.add(callback);
			return insertHookCode(declaringClass, method, retExpr);
		} catch (Exception e) {
			log(e);
			return null;
		}
	}

	@SuppressWarnings({"unused"})
	public static void handleBeforeHookedConstructor(final int methodId, final Member originalMethod, Object thisObject, Object[] args) throws Throwable {
		CopyOnWriteSortedSet<XCallback> set = ctHookedMethodCallbacks.get(methodId);
		Object[] callbacksSnapshot = set == null ? new Object[0] : set.getSnapshot();
		final int callbacksLength = callbacksSnapshot.length;
		if (callbacksLength == 0) {
			return;
		}

		if (originalMethod instanceof Constructor) {
			final XC_ConstructorHook.ConstructorBeforeHookParam param = new XC_ConstructorHook.ConstructorBeforeHookParam ();
			param.constructor = (Constructor<?>) originalMethod;
			param.thisClass = (Class<?>) thisObject;

			// call "before method" callbacks
			int beforeIdx = 0;
			do {
				XC_ConstructorHook hook = (XC_ConstructorHook) callbacksSnapshot[beforeIdx];
				hook.beforeHookedConstructor(param);
			} while (++beforeIdx < callbacksLength);
		} else {
			final XC_ClassInitializerHook.ClassInitializerHookParam param = new XC_ClassInitializerHook.ClassInitializerHookParam();
			param.thisClass = (Class<?>) thisObject;

			// call "before method" callbacks
			int beforeIdx = 0;
			do {
				XC_ClassInitializerHook hook = (XC_ClassInitializerHook) callbacksSnapshot[beforeIdx];
				hook.beforeHookedClassInitializer(param);
			} while (++beforeIdx < callbacksLength);
		}
	}

	@SuppressWarnings({"unused"})
	public static void handleAfterHookedConstructor(final int methodId, final Member originalMethod, Object thisObject, Object[] args) throws Throwable {
		CopyOnWriteSortedSet<XCallback> set = ctHookedMethodCallbacks.get(methodId);
		Object[] callbacksSnapshot = set == null ? new Object[0] : set.getSnapshot();
		final int callbacksLength = callbacksSnapshot.length;
		if (callbacksLength == 0) {
			return;
		}

		if (originalMethod instanceof Constructor) {
			final XC_ConstructorHook.ConstructorAfterHookParam param = new XC_ConstructorHook.ConstructorAfterHookParam();
			param.constructor = (Constructor<?>) originalMethod;
			param.thisObject = thisObject;

			// call "after method" callbacks
			int afterIdx = callbacksLength - 1;
			do {
				XC_ConstructorHook hook = (XC_ConstructorHook) callbacksSnapshot[afterIdx];
				hook.afterHookedConstructor(param);
			} while (--afterIdx >= 0);
		} else {
			final XC_ClassInitializerHook.ClassInitializerHookParam param = new XC_ClassInitializerHook.ClassInitializerHookParam();
			param.thisClass = (Class<?>) thisObject;

			// call "after method" callbacks
			int afterIdx = callbacksLength - 1;
			do {
				XC_ClassInitializerHook hook = (XC_ClassInitializerHook) callbacksSnapshot[afterIdx];
				hook.afterHookedClassInitializer(param);
			} while (--afterIdx >= 0);
		}
	}

	synchronized static void hookClassInitializer(CtClass cc, CtConstructor classInitializer, XC_ClassInitializerHook callback) throws CannotCompileException {
		// unfreeze the class so we can modify it
		cc.defrost();

		final String clazz = cc.getName() + ".class";
		final int methodId = Objects.hashCode(classInitializer.getLongName());
		String handleBeforeHookedConstructor = "de.robv.android.xposed.XposedBridge.handleBeforeHookedConstructor(" + methodId + ", null, " + clazz + ", new Object[0]);\n";
		String handleAfterHookedConstructor = "de.robv.android.xposed.XposedBridge.handleAfterHookedConstructor(" + methodId + ", null, " + clazz + ", new Object[0]);\n";

		CopyOnWriteSortedSet<XCallback> callbacks;
		synchronized (ctHookedMethodCallbacks) {
			callbacks = ctHookedMethodCallbacks.get(methodId);
			if (callbacks == null) {
				callbacks = new CopyOnWriteSortedSet<>();
				ctHookedMethodCallbacks.put(methodId, callbacks);
			}
		}
		callbacks.add(callback);

		classInitializer.insertBefore("{\n" +
				handleBeforeHookedConstructor +
				"}");
		classInitializer.insertAfter("{\n" +
				handleAfterHookedConstructor +
				"}");
	}

	synchronized static void hookConstructor(CtClass cc, CtConstructor constructor, XC_ConstructorHook callback) throws CannotCompileException, NotFoundException {
		// unfreeze the class so we can modify it
		cc.defrost();

		final String clazz = cc.getName() + ".class";
		final int methodId = Objects.hashCode(constructor.getLongName());
		String handleBeforeHookedConstructor = "de.robv.android.xposed.XposedBridge.handleBeforeHookedConstructor(" + methodId + ", constructor, " + clazz + ", $args);\n";
		String handleAfterHookedConstructor = "de.robv.android.xposed.XposedBridge.handleAfterHookedConstructor(" + methodId + ", constructor, $0, $args);\n";

		CopyOnWriteSortedSet<XCallback> callbacks;
		synchronized (ctHookedMethodCallbacks) {
			callbacks = ctHookedMethodCallbacks.get(methodId);
			if (callbacks == null) {
				callbacks = new CopyOnWriteSortedSet<>();
				ctHookedMethodCallbacks.put(methodId, callbacks);
			}
		}
		callbacks.add(callback);

		final String sig;
		CtClass[] types = constructor.getParameterTypes();
		if(types.length < 1) {
			sig = ", new Class[0]";
		} else {
			StringBuilder builder = new StringBuilder();
			for(CtClass type : types) {
				builder.append(type.getName()).append(".class").append(',');
			}
			builder.deleteCharAt(builder.length() - 1);
			sig = ", new Class[] { " + builder + " }";
		}

		constructor.insertBefore("{\n" +
				"java.lang.reflect.Member constructor = de.robv.android.xposed.XposedHelpers.findConstructorBestMatch(" + cc.getName() + ".class " + sig + ");\n" +
				handleBeforeHookedConstructor +
				"}");
		constructor.insertAfter("{\n" +
				"java.lang.reflect.Member constructor = de.robv.android.xposed.XposedHelpers.findConstructorBestMatch(" + cc.getName() + ".class " + sig + ");\n" +
				handleAfterHookedConstructor +
				"}");
	}

	/**
	 * Intercept every call to the specified method and call a handler function instead.
	 * @param method The method to intercept
	 */
	synchronized static void hookMethod(CtClass cc, CtMethod method, XC_MethodHook callback) throws NotFoundException, CannotCompileException {
		ClassPool cp = ClassPool.getDefault();
		CtClass returnType = method.getReturnType();

		// unfreeze the class so we can modify it
		cc.defrost();

		String thiz;
		if(Modifier.isStatic(method.getModifiers())) {
			thiz = cc.getName() + ".class";
		} else {
			thiz = "$0";
		}

		final int methodId = Objects.hashCode(method.getLongName() + method.getReturnType().getName());
		String retExpr;
		if(returnType == CtClass.voidType) {
			retExpr = "de.robv.android.xposed.XposedBridge.handleHookedMethod(" + methodId + ", method, " + thiz + ", $args);return;\n";
		} else {
			retExpr = "Object ret = de.robv.android.xposed.XposedBridge.handleHookedMethod(" + methodId + ", method, " + thiz + ", $args);";

			if(returnType == CtClass.booleanType) {
				retExpr += "return ((Boolean) ret).booleanValue();\n";
			} else if(returnType == CtClass.byteType) {
				retExpr += "return ((Byte) ret).byteValue();\n";
			} else if(returnType == CtClass.intType) {
				retExpr += "return ((Integer) ret).intValue();\n";
			} else if(returnType == CtClass.charType) {
				retExpr += "return ((Character) ret).charValue();\n";
			} else if(returnType == CtClass.shortType) {
				retExpr += "return ((Short) ret).shortValue();\n";
			} else if(returnType == CtClass.longType) {
				retExpr += "return ((Long) ret).longValue();\n";
			} else if(returnType == CtClass.floatType) {
				retExpr += "return ((Float) ret).floatValue();\n";
			} else if(returnType == CtClass.doubleType) {
				retExpr += "return ((Double) ret).doubleValue();\n";
			} else if(returnType != cp.get("java.lang.Object")) {
				retExpr += "return (" + returnType.getName() + ") ret;\n";
			} else {
				retExpr += "return ret;\n";
			}
		}

		CopyOnWriteSortedSet<XCallback> callbacks;
		synchronized (ctHookedMethodCallbacks) {
			callbacks = ctHookedMethodCallbacks.get(methodId);
			if (callbacks == null) {
				callbacks = new CopyOnWriteSortedSet<>();
				ctHookedMethodCallbacks.put(methodId, callbacks);
			}
		}
		callbacks.add(callback);

		final String sig;
		CtClass[] types = method.getParameterTypes();
		if(types.length < 1) {
			sig = ", new Class[0]";
		} else {
			StringBuilder builder = new StringBuilder();
			for(CtClass type : types) {
				builder.append(type.getName()).append(".class").append(',');
			}
			builder.deleteCharAt(builder.length() - 1);
			sig = ", new Class[] { " + builder + " }";
		}

		method.insertBefore("{\n" +
				"java.lang.reflect.Method method = de.robv.android.xposed.XposedHelpers.findMethodBestMatch(" + cc.getName() + ".class, \"" + method.getName() + "\"" + sig + ");\n" +
				"if(!de.robv.android.xposed.XposedBridge.isBridgeInvoked(method)) {\n" +
				retExpr +
				"}\n" +
				"}");
	}

	private static final Map<Integer, CopyOnWriteSortedSet<XCallback>> ctHookedMethodCallbacks = new HashMap<>();

	/**
	 * This method is called as a replacement for hooked methods.
	 */
	@SuppressWarnings({"unused", "unchecked"})
	public static Object handleHookedMethod(final int methodId, final Method originalMethod, Object thisObject, Object[] args) throws Throwable {
		CopyOnWriteSortedSet<XCallback> set = ctHookedMethodCallbacks.get(methodId);
		Object[] callbacksSnapshot = set == null ? new Object[0] : set.getSnapshot();
		final int callbacksLength = callbacksSnapshot.length;
		if (callbacksLength == 0) {
			try {
				return invokeOriginalMethodNative(originalMethod, thisObject, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		final MethodHookParam param = new MethodHookParam();
		param.method = originalMethod;
		param.thisObject = thisObject;
		param.args = args;

		XCMethodPointer<Object, Object> old = null;

		// call "before method" callbacks
		int beforeIdx = 0;
		do {
			try {
				XC_MethodHook hook = (XC_MethodHook) callbacksSnapshot[beforeIdx];
				if(!(hook instanceof XC_MethodHookAlteration)) {
					hook.beforeHookedMethod(param);
					continue;
				}

				if(old == null) {
					old = new XCMethodPointer<Object, Object>() {
						@Override
						public Object invoke(Object thisObj, Object... args) throws Throwable {
							return invokeOriginalMethodNative(originalMethod, thisObj, args);
						}
						@Override
						public Class<?>[] getParameterTypes() {
							return originalMethod.getParameterTypes();
						}
						@Override
						public Class<?> getDeclaringClass() {
							return param.method.getDeclaringClass();
						}
						@Override
						public Member getMethod() {
							return param.method;
						}
					};
				}
				param.setResult(((XC_MethodHookAlteration<Object, Object>) hook).invoked(old, param.thisObject, param));
				param.returnEarly = true;
			} catch (Throwable t) {
				XposedBridge.log(t);

				// reset result (ignoring what the unexpectedly exiting callback did)
				param.setResult(null);
				param.returnEarly = false;
			}

			if (param.returnEarly) {
				// skip remaining "before" callbacks and corresponding "after" callbacks
				beforeIdx++;
				break;
			}
		} while (++beforeIdx < callbacksLength);

		// call original method if not requested otherwise
		if (!param.returnEarly) {
			try {
				param.setResult(invokeOriginalMethodNative(originalMethod, param.thisObject, param.args));
			} catch (InvocationTargetException e) {
				param.setThrowable(e.getCause());
			}
		}

		// call "after method" callbacks
		int afterIdx = beforeIdx - 1;
		do {
			Object lastResult =  param.getResult();
			Throwable lastThrowable = param.getThrowable();

			try {
				XC_MethodHook hook = (XC_MethodHook) callbacksSnapshot[afterIdx];

				if(!(hook instanceof XC_MethodHookAlteration)) {
					hook.afterHookedMethod(param);
				}
			} catch (Throwable t) {
				XposedBridge.log(t);

				// reset to last result (ignoring what the unexpectedly exiting callback did)
				if (lastThrowable == null)
					param.setResult(lastResult);
				else
					param.setThrowable(lastThrowable);
			}
		} while (--afterIdx >= 0);

		// return
		if (param.hasThrowable()) {
			throw param.getThrowable();
		} else {
			return param.getResult();
		}
	}

	private static byte[] insertHookCode(CtClass cc, CtMethod cm, String retExpr) throws NotFoundException, CannotCompileException, IOException {
		final String sig;
		CtClass[] types = cm.getParameterTypes();
		if(types.length < 1) {
			sig = ", new Class[0]";
		} else {
			StringBuilder builder = new StringBuilder();
			for(CtClass type : types) {
				builder.append(type.getName()).append(".class").append(',');
			}
			builder.deleteCharAt(builder.length() - 1);
			sig = ", new Class[] { " + builder + " }";
		}

		cm.insertBefore("{\n" +
				"java.lang.reflect.Method method = de.robv.android.xposed.XposedHelpers.findMethodBestMatch(" + cc.getName() + ".class, \"" + cm.getName() + "\"" + sig + ");\n" +
				"if(!de.robv.android.xposed.XposedBridge.isBridgeInvoked(method)) {\n" +
				retExpr +
				"}\n" +
				"}");

		// save the instrumented version of the class
		byte[] newByteCode = cc.toBytecode();
		if(debug) {
			cc.writeFile(System.getProperty("user.home") + "/.snoop/tmp");
		}
		return newByteCode;
	}
	
	private static boolean debug;
	
	public static void setDebug() {
		debug = true;
	}

	private static Object invokeOriginalMethodNative(Method originalMethod, Object thisObject, Object[] args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		originalMethod.setAccessible(true);
		return originalMethod.invoke(Modifier.isStatic(originalMethod.getModifiers()) ? null : thisObject, args);
	}

	public static class CopyOnWriteSortedSet<E> {
		private transient volatile Object[] elements = EMPTY_ARRAY;

		public synchronized boolean add(E e) {
			int index = indexOf(e);
			if (index >= 0)
				return false;

			Object[] newElements = new Object[elements.length + 1];
			System.arraycopy(elements, 0, newElements, 0, elements.length);
			newElements[elements.length] = e;
			Arrays.sort(newElements);
			elements = newElements;
			return true;
		}

		public synchronized boolean remove(E e) {
			int index = indexOf(e);
			if (index == -1)
				return false;

			Object[] newElements = new Object[elements.length - 1];
			System.arraycopy(elements, 0, newElements, 0, index);
			System.arraycopy(elements, index + 1, newElements, index, elements.length - index - 1);
			elements = newElements;
			return true;
		}

		private int indexOf(Object o) {
			for (int i = 0; i < elements.length; i++) {
				if (o.equals(elements[i]))
					return i;
			}
			return -1;
		}

		public Object[] getSnapshot() {
			return elements;
		}
	}
}

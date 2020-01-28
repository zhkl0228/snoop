package de.robv.android.xposed;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

public class XposedHelpers {
	private static final HashMap<String, Field> fieldCache = new HashMap<String, Field>();
	private static final HashMap<String, Method> methodCache = new HashMap<String, Method>();
	private static final HashMap<String, Constructor<?>> constructorCache = new HashMap<String, Constructor<?>>();
	private static final WeakHashMap<Object, HashMap<String, Object>> additionalFields = new WeakHashMap<Object, HashMap<String, Object>>();

	/**
	 * Look up a class with the specified class loader (or the boot class loader if
	 * <code>classLoader</code> is <code>null</code>).
	 * <p>Class names can be specified in different formats:
	 * <ul><li>java.lang.Integer
	 * <li>int
	 * <li>int[]
	 * <li>[I
	 * <li>java.lang.String[]
	 * <li>[Ljava.lang.String;
	 * <li>android.app.ActivityThread.ResourcesKey
	 * <li>android.app.ActivityThread$ResourcesKey
	 * <li>android.app.ActivityThread$ResourcesKey[]</ul>
	 * <p>A {@link ClassNotFoundError} is thrown in case the class was not found.
	 */
	public static Class<?> findClass(String className, ClassLoader classLoader) {
		if (classLoader == null)
			classLoader = ClassLoader.getSystemClassLoader();
		try {
			return Class.forName(className, false, classLoader);
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundError(e);
		}
	}

	/**
	 * Look up a field in a class and set it to accessible. The result is cached.
	 * If the field was not found, a {@link NoSuchFieldError} will be thrown.
	 */
	public static Field findField(Class<?> clazz, String fieldName) {
		StringBuilder sb = new StringBuilder(clazz.getName());
		sb.append('#');
		sb.append(fieldName);
		String fullFieldName = sb.toString();

		if (fieldCache.containsKey(fullFieldName)) {
			Field field = fieldCache.get(fullFieldName);
			if (field == null)
				throw new NoSuchFieldError(fullFieldName);
			return field;
		}

		try {
			Field field = findFieldRecursiveImpl(clazz, fieldName);
			field.setAccessible(true);
			fieldCache.put(fullFieldName, field);
			return field;
		} catch (NoSuchFieldException e) {
			fieldCache.put(fullFieldName, null);
			throw new NoSuchFieldError(fullFieldName);
		}
	}

	private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			while (true) {
				clazz = clazz.getSuperclass();
				if (clazz == null || clazz.equals(Object.class))
					break;

				try {
					return clazz.getDeclaredField(fieldName);
				} catch (NoSuchFieldException ignored) {}
			}
			throw e;
		}
	}

	/**
	 * Returns the first field of the given type in a class.
	 * Might be useful for Proguard'ed classes to identify fields with unique types.
	 * If no matching field was not found, a {@link NoSuchFieldError} will be thrown.
	 */
	public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
		Class<?> clz = clazz;
		do {
			for (Field field : clz.getDeclaredFields()) {
				if (field.getType() == type) {
					field.setAccessible(true);
					return field;
				}
			}
		} while ((clz = clz.getSuperclass()) != null);

		throw new NoSuchFieldError("Field of type " + type.getName() + " in class " + clazz.getName());
	}

	/**
	 * Look up a method and place a hook on it. The last argument must be the callback for the hook.
	 * @see #findMethodExact(Class, String, Object...)
	 */
	public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
		if (parameterTypesAndCallback.length == 0 || !(parameterTypesAndCallback[parameterTypesAndCallback.length-1] instanceof XC_MethodHook))
			throw new IllegalArgumentException("no callback defined");

		XC_MethodHook callback = (XC_MethodHook) parameterTypesAndCallback[parameterTypesAndCallback.length-1];
		Method m = findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));

		XposedBridge.hookMethod(m, callback);
	}

	/** @see #findAndHookMethod(Class, String, Object...) */
	public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
		findAndHookMethod(findClass(className, classLoader), methodName, parameterTypesAndCallback);
	}

	/**
	 * Look up a method in a class and set it to accessible. The result is cached.
	 * If the method was not found, a {@link NoSuchMethodError} will be thrown.
	 *
	 * <p>The parameter types may either be specified as <code>Class</code> or <code>String</code>
	 * objects. In the latter case, the class is looked up using {@link #findClass} with the same
	 * class loader as the method's class.
	 */
	public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
		return findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypes));
	}

	/** @see #findMethodExact(Class, String, Object...) */
	public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
		return findMethodExact(findClass(className, classLoader), methodName, getParameterClasses(classLoader, parameterTypes));
	}

	/** @see #findMethodExact(Class, String, Object...) */
	public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		StringBuilder sb = new StringBuilder(clazz.getName());
		sb.append('#');
		sb.append(methodName);
		sb.append(getParametersString(parameterTypes));
		sb.append("#exact");
		String fullMethodName = sb.toString();

		if (methodCache.containsKey(fullMethodName)) {
			Method method = methodCache.get(fullMethodName);
			if (method == null)
				throw new NoSuchMethodError(fullMethodName);
			return method;
		}

		try {
			Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
			method.setAccessible(true);
			methodCache.put(fullMethodName, method);
			return method;
		} catch (NoSuchMethodException e) {
			methodCache.put(fullMethodName, null);
			throw new NoSuchMethodError(fullMethodName);
		}
	}

	/**
	 * Returns an array of all methods in a class with the specified parameter types.
	 *
	 * The return type is optional, it will not be compared if it is {@code null}.
	 * Use {@code void.class} if you want to search for methods returning nothing.
	 */
	public static Method[] findMethodsByExactParameters(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
		List<Method> result = new LinkedList<Method>();
		for (Method method : clazz.getDeclaredMethods()) {
			if (returnType != null && returnType != method.getReturnType())
				continue;

			Class<?>[] methodParameterTypes = method.getParameterTypes();
			if (parameterTypes.length != methodParameterTypes.length)
				continue;

			boolean match = true;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (parameterTypes[i] != methodParameterTypes[i]) {
					match = false;
					break;
				}
			}

			if (!match)
				continue;

			method.setAccessible(true);
			result.add(method);
		}
		return result.toArray(new Method[0]);
	}

	/**
	 * Look up a method in a class and set it to accessible. The result is cached.
	 * This does not only look for exact matches, but for the closest match.
	 * If the method was not found, a {@link NoSuchMethodError} will be thrown.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		String fullMethodName = clazz.getName() + '#' +
				methodName +
				getParametersString(parameterTypes) +
				"#bestmatch";

		if (methodCache.containsKey(fullMethodName)) {
			Method method = methodCache.get(fullMethodName);
			if (method == null)
				throw new NoSuchMethodError(fullMethodName);
			return method;
		}

		try {
			Method method = findMethodExact(clazz, methodName, parameterTypes);
			methodCache.put(fullMethodName, method);
			return method;
		} catch (NoSuchMethodError ignored) {}

		Method bestMatch = null;
		Class<?> clz = clazz;
		boolean considerPrivateMethods = true;
		do {
			for (Method method : clz.getDeclaredMethods()) {
				// don't consider private methods of superclasses
				if (!considerPrivateMethods && Modifier.isPrivate(method.getModifiers()))
					continue;

				// compare name and parameters
				if (method.getName().equals(methodName) && isAssignable(parameterTypes, method.getParameterTypes())) {
					// get accessible version of method
					if (bestMatch == null || MemberUtils.compareParameterTypes(
							method.getParameterTypes(),
							bestMatch.getParameterTypes(),
							parameterTypes) < 0) {
						bestMatch = method;
					}
				}
			}
			considerPrivateMethods = false;
		} while ((clz = clz.getSuperclass()) != null);

		if (bestMatch != null) {
			bestMatch.setAccessible(true);
			methodCache.put(fullMethodName, bestMatch);
			return bestMatch;
		} else {
			NoSuchMethodError e = new NoSuchMethodError(fullMethodName);
			methodCache.put(fullMethodName, null);
			throw e;
		}
	}

	/**
	 * Look up a method in a class and set it to accessible. Parameter types are
	 * determined from the <code>args</code> for the method call. The result is cached.
	 * This does not only look for exact matches, but for the closest match.
	 * If the method was not found, a {@link NoSuchMethodError} will be thrown.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName) {
		return findMethodBestMatch(clazz, methodName, new Class<?>[0]);
	}

	/**
	 * Look up a method in a class and set it to accessible. Parameter types are
	 * determined from the <code>args</code> for the method call. The result is cached.
	 * This does not only look for exact matches, but for the closest match.
	 * If the method was not found, a {@link NoSuchMethodError} will be thrown.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
		return findMethodBestMatch(clazz, methodName, getParameterTypes(args));
	}

	/**
	 * Look up a method in a class and set it to accessible. Parameter types are
	 * preferably taken from the <code>parameterTypes</code>. Any item in this array that
	 * is <code>null</code> is determined from the corresponding item in <code>args</code>.
	 * The result is cached.
	 * This does not only look for exact matches, but for the closest match.
	 * If the method was not found, a {@link NoSuchMethodError} will be thrown.
	 */
	public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
		Class<?>[] argsClasses = null;
		for (int i = 0; i < parameterTypes.length; i++) {
			if (parameterTypes[i] != null)
				continue;
			if (argsClasses == null)
				argsClasses = getParameterTypes(args);
			parameterTypes[i] = argsClasses[i];
		}
		return findMethodBestMatch(clazz, methodName, parameterTypes);
	}

	/**
	 * Return an array with the classes of the given objects
	 */
	public static Class<?>[] getParameterTypes(Object... args) {
		Class<?>[] clazzes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			clazzes[i] = (args[i] != null) ? args[i].getClass() : null;
		}
		return clazzes;
	}

	/**
	 * Retrieve classes from an array, where each element might either be a Class
	 * already, or a String with the full class name.
	 */
	private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
		Class<?>[] parameterClasses = null;
		for (int i = parameterTypesAndCallback.length - 1; i >= 0; i--) {
			Object type = parameterTypesAndCallback[i];
			if (type == null)
				throw new ClassNotFoundError("parameter type must not be null", null);

			// ignore trailing callback
			if (type instanceof XC_MethodHook)
				continue;

			if (parameterClasses == null)
				parameterClasses = new Class<?>[i+1];

			if (type instanceof Class)
				parameterClasses[i] = (Class<?>) type;
			else if (type instanceof String)
				parameterClasses[i] = findClass((String) type, classLoader);
			else
				throw new ClassNotFoundError("parameter type must either be specified as Class or String", null);
		}

		// if there are no arguments for the method
		if (parameterClasses == null)
			parameterClasses = new Class<?>[0];

		return parameterClasses;
	}

	/**
	 * Return an array with the classes of the given objects
	 */
	public static Class<?>[] getClassesAsArray(Class<?>... clazzes) {
		return clazzes;
	}

	private static String getParametersString(Class<?>... clazzes) {
		StringBuilder sb = new StringBuilder("(");
		boolean first = true;
		for (Class<?> clazz : clazzes) {
			if (first)
				first = false;
			else
				sb.append(",");

			if (clazz != null)
				sb.append(clazz.getCanonicalName());
			else
				sb.append("null");
		}
		sb.append(")");
		return sb.toString();
	}

	public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
		return findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypes));
	}

	public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
		return findConstructorExact(findClass(className, classLoader), getParameterClasses(classLoader, parameterTypes));
	}

	public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
		StringBuilder sb = new StringBuilder(clazz.getName());
		sb.append(getParametersString(parameterTypes));
		sb.append("#exact");
		String fullConstructorName = sb.toString();

		if (constructorCache.containsKey(fullConstructorName)) {
			Constructor<?> constructor = constructorCache.get(fullConstructorName);
			if (constructor == null)
				throw new NoSuchMethodError(fullConstructorName);
			return constructor;
		}

		try {
			Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			constructorCache.put(fullConstructorName, constructor);
			return constructor;
		} catch (NoSuchMethodException e) {
			constructorCache.put(fullConstructorName, null);
			throw new NoSuchMethodError(fullConstructorName);
		}
	}

	public static void findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
		if (parameterTypesAndCallback.length != 0 && parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_ConstructorHook) {
			XC_ConstructorHook callback = (XC_ConstructorHook)parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
			Constructor<?> m = findConstructorExact(clazz, getParameterClasses(clazz.getClassLoader(), parameterTypesAndCallback));
			XposedBridge.hookMethod(m, callback);
		} else {
			throw new IllegalArgumentException("no callback defined");
		}
	}

	public static void findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
		findAndHookConstructor(findClass(className, classLoader), parameterTypesAndCallback);
	}

	/**
	 * <p>Checks whether two arrays are the same length, treating
	 * {@code null} arrays as length {@code 0}.
	 *
	 * <p>Any multi-dimensional aspects of the arrays are ignored.</p>
	 *
	 * @param array1 the first array, may be {@code null}
	 * @param array2 the second array, may be {@code null}
	 * @return {@code true} if length of arrays matches, treating
	 *  {@code null} as an empty array
	 */
	private static boolean isSameLength(final Object[] array1, final Object[] array2) {
		return (array1 != null || array2 == null || array2.length <= 0) &&
				(array2 != null || array1 == null || array1.length <= 0) &&
				(array1 == null || array2 == null || array1.length == array2.length);
	}

	/**
	 * <p>Checks if an array of Classes can be assigned to another array of Classes.</p>
	 *
	 * <p>This method calls {@link MemberUtils#isAssignable(Class, Class) isAssignable} for each
	 * Class pair in the input arrays. It can be used to check if a set of arguments
	 * (the first parameter) are suitably compatible with a set of method parameter types
	 * (the second parameter).</p>
	 *
	 * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method, this
	 * method takes into account widenings of primitive classes and
	 * {@code null}s.</p>
	 *
	 * <p>Primitive widenings allow an int to be assigned to a {@code long},
	 * {@code float} or {@code double}. This method returns the correct
	 * result for these cases.</p>
	 *
	 * <p>{@code Null} may be assigned to any reference type. This method will
	 * return {@code true} if {@code null} is passed in and the toClass is
	 * non-primitive.</p>
	 *
	 * <p>Specifically, this method tests whether the type represented by the
	 * specified {@code Class} parameter can be converted to the type
	 * represented by this {@code Class} object via an identity conversion
	 * widening primitive or widening reference conversion. See
	 * <em><a href="http://docs.oracle.com/javase/specs/">The Java Language Specification</a></em>,
	 * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
	 *
	 * @param classArray  the array of Classes to check, may be {@code null}
	 * @param toClassArray  the array of Classes to try to assign into, may be {@code null}
	 * @return {@code true} if assignment possible
	 */
	private static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray) {
		if (!isSameLength(classArray, toClassArray)) {
			return false;
		}
		if (classArray == null) {
			classArray = new Class<?>[0];
		}
		if (toClassArray == null) {
			toClassArray = new Class<?>[0];
		}
		for (int i = 0; i < classArray.length; i++) {
			if (!MemberUtils.isAssignable(classArray[i], toClassArray[i])) {
				return false;
			}
		}
		return true;
	}

	public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
		StringBuilder sb = new StringBuilder(clazz.getName());
		sb.append(getParametersString(parameterTypes));
		sb.append("#bestmatch");
		String fullConstructorName = sb.toString();

		if (constructorCache.containsKey(fullConstructorName)) {
			Constructor<?> constructor = constructorCache.get(fullConstructorName);
			if (constructor == null)
				throw new NoSuchMethodError(fullConstructorName);
			return constructor;
		}

		try {
			Constructor<?> constructor = findConstructorExact(clazz, parameterTypes);
			constructorCache.put(fullConstructorName, constructor);
			return constructor;
		} catch (NoSuchMethodError ignored) {}

		Constructor<?> bestMatch = null;
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		for (Constructor<?> constructor : constructors) {
			// compare name and parameters
			if (isAssignable(parameterTypes, constructor.getParameterTypes())) {
				// get accessible version of method
				if (bestMatch == null || MemberUtils.compareParameterTypes(
						constructor.getParameterTypes(),
						bestMatch.getParameterTypes(),
						parameterTypes) < 0) {
					bestMatch = constructor;
				}
			}
		}

		if (bestMatch != null) {
			bestMatch.setAccessible(true);
			constructorCache.put(fullConstructorName, bestMatch);
			return bestMatch;
		} else {
			NoSuchMethodError e = new NoSuchMethodError(fullConstructorName);
			constructorCache.put(fullConstructorName, null);
			throw e;
		}
	}

	public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
		return findConstructorBestMatch(clazz, getParameterTypes(args));
	}

	public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
		Class<?>[] argsClasses = null;
		for (int i = 0; i < parameterTypes.length; i++) {
			if (parameterTypes[i] != null)
				continue;
			if (argsClasses == null)
				argsClasses = getParameterTypes(args);
			parameterTypes[i] = argsClasses[i];
		}
		return findConstructorBestMatch(clazz, parameterTypes);
	}

	public static class ClassNotFoundError extends Error {
		private static final long serialVersionUID = -1070936889459514628L;
		public ClassNotFoundError(Throwable cause) {
			super(cause);
		}
		public ClassNotFoundError(String detailMessage, Throwable cause) {
			super(detailMessage, cause);
		}
	}

	//#################################################################################################
	public static void setObjectField(Object obj, String fieldName, Object value) {
		try {
			findField(obj.getClass(), fieldName).set(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setBooleanField(Object obj, String fieldName, boolean value) {
		try {
			findField(obj.getClass(), fieldName).setBoolean(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setByteField(Object obj, String fieldName, byte value) {
		try {
			findField(obj.getClass(), fieldName).setByte(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setCharField(Object obj, String fieldName, char value) {
		try {
			findField(obj.getClass(), fieldName).setChar(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setDoubleField(Object obj, String fieldName, double value) {
		try {
			findField(obj.getClass(), fieldName).setDouble(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setFloatField(Object obj, String fieldName, float value) {
		try {
			findField(obj.getClass(), fieldName).setFloat(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setIntField(Object obj, String fieldName, int value) {
		try {
			findField(obj.getClass(), fieldName).setInt(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setLongField(Object obj, String fieldName, long value) {
		try {
			findField(obj.getClass(), fieldName).setLong(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setShortField(Object obj, String fieldName, short value) {
		try {
			findField(obj.getClass(), fieldName).setShort(obj, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	public static Object getObjectField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).get(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/** For inner classes, return the "this" reference of the surrounding class */
	public static Object getSurroundingThis(Object obj) {
		return getObjectField(obj, "this$0");
	}

	public static boolean getBooleanField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getBoolean(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static byte getByteField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getByte(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static char getCharField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getChar(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static double getDoubleField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getDouble(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static float getFloatField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getFloat(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static int getIntField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getInt(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static long getLongField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getLong(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static short getShortField(Object obj, String fieldName) {
		try {
			return findField(obj.getClass(), fieldName).getShort(obj);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
		try {
			findField(clazz, fieldName).set(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
		try {
			findField(clazz, fieldName).setBoolean(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticByteField(Class<?> clazz, String fieldName, byte value) {
		try {
			findField(clazz, fieldName).setByte(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticCharField(Class<?> clazz, String fieldName, char value) {
		try {
			findField(clazz, fieldName).setChar(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticDoubleField(Class<?> clazz, String fieldName, double value) {
		try {
			findField(clazz, fieldName).setDouble(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticFloatField(Class<?> clazz, String fieldName, float value) {
		try {
			findField(clazz, fieldName).setFloat(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
		try {
			findField(clazz, fieldName).setInt(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticLongField(Class<?> clazz, String fieldName, long value) {
		try {
			findField(clazz, fieldName).setLong(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static void setStaticShortField(Class<?> clazz, String fieldName, short value) {
		try {
			findField(clazz, fieldName).setShort(null, value);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).get(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getBoolean(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static byte getStaticByteField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getByte(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static char getStaticCharField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getChar(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static double getStaticDoubleField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getDouble(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static float getStaticFloatField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getFloat(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static int getStaticIntField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getInt(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static long getStaticLongField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getLong(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public static short getStaticShortField(Class<?> clazz, String fieldName) {
		try {
			return findField(clazz, fieldName).getShort(null);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	//#################################################################################################
	/**
	 * Call instance or static method <code>methodName</code> for object <code>obj</code> with the arguments
	 * <code>args</code>. The types for the arguments will be determined automaticall from <code>args</code>
	 */
	public static Object callMethod(Object obj, String methodName, Object... args) {
		try {
			return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e);
		}
	}

	/**
	 * Call instance or static method <code>methodName</code> for object <code>obj</code> with the arguments
	 * <code>args</code>. The types for the arguments will be taken from <code>parameterTypes</code>.
	 * This array can have items that are <code>null</code>. In this case, the type for this parameter
	 * is determined from <code>args</code>.
	 */
	public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
		try {
			return findMethodBestMatch(obj.getClass(), methodName, parameterTypes, args).invoke(obj, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	/**
	 * Call static method <code>methodName</code> for class <code>clazz</code> with the arguments
	 * <code>args</code>. The types for the arguments will be determined automaticall from <code>args</code>
	 */
	public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
		try {
			return findMethodBestMatch(clazz, methodName, args).invoke(null, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	/**
	 * Call static method <code>methodName</code> for class <code>clazz</code> with the arguments
	 * <code>args</code>. The types for the arguments will be taken from <code>parameterTypes</code>.
	 * This array can have items that are <code>null</code>. In this case, the type for this parameter
	 * is determined from <code>args</code>.
	 */
	public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
		try {
			return findMethodBestMatch(clazz, methodName, parameterTypes, args).invoke(null, args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		}
	}

	public static class InvocationTargetError extends Error {
		private static final long serialVersionUID = -1070936889459514628L;
		public InvocationTargetError(Throwable cause) {
			super(cause);
		}
		public InvocationTargetError(String detailMessage, Throwable cause) {
			super(detailMessage, cause);
		}
	}

	//#################################################################################################
	public static Object newInstance(Class<?> clazz, Object... args) {
		try {
			return findConstructorBestMatch(clazz, args).newInstance(args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		} catch (InstantiationException e) {
			throw new InstantiationError(e.getMessage());
		}
	}

	public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
		try {
			return findConstructorBestMatch(clazz, parameterTypes, args).newInstance(args);
		} catch (IllegalAccessException e) {
			// should not happen
			XposedBridge.log(e);
			throw new IllegalAccessError(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw new InvocationTargetError(e.getCause());
		} catch (InstantiationException e) {
			throw new InstantiationError(e.getMessage());
		}
	}

	//#################################################################################################
	public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
		if (obj == null)
			throw new NullPointerException("object must not be null");
		if (key == null)
			throw new NullPointerException("key must not be null");

		HashMap<String, Object> objectFields;
		synchronized (additionalFields) {
			objectFields = additionalFields.get(obj);
			if (objectFields == null) {
				objectFields = new HashMap<String, Object>();
				additionalFields.put(obj, objectFields);
			}
		}

		synchronized (objectFields) {
			return objectFields.put(key, value);
		}
	}

	public static Object getAdditionalInstanceField(Object obj, String key) {
		if (obj == null)
			throw new NullPointerException("object must not be null");
		if (key == null)
			throw new NullPointerException("key must not be null");

		HashMap<String, Object> objectFields;
		synchronized (additionalFields) {
			objectFields = additionalFields.get(obj);
			if (objectFields == null)
				return null;
		}

		synchronized (objectFields) {
			return objectFields.get(key);
		}
	}

	public static Object removeAdditionalInstanceField(Object obj, String key) {
		if (obj == null)
			throw new NullPointerException("object must not be null");
		if (key == null)
			throw new NullPointerException("key must not be null");

		HashMap<String, Object> objectFields;
		synchronized (additionalFields) {
			objectFields = additionalFields.get(obj);
			if (objectFields == null)
				return null;
		}

		synchronized (objectFields) {
			return objectFields.remove(key);
		}
	}

	public static Object setAdditionalStaticField(Object obj, String key, Object value) {
		return setAdditionalInstanceField(obj.getClass(), key, value);
	}

	public static Object getAdditionalStaticField(Object obj, String key) {
		return getAdditionalInstanceField(obj.getClass(), key);
	}

	public static Object removeAdditionalStaticField(Object obj, String key) {
		return removeAdditionalInstanceField(obj.getClass(), key);
	}

	public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
		return setAdditionalInstanceField(clazz, key, value);
	}

	public static Object getAdditionalStaticField(Class<?> clazz, String key) {
		return getAdditionalInstanceField(clazz, key);
	}

	public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
		return removeAdditionalInstanceField(clazz, key);
	}

	/**
	 * Returns the lowercase string representation of the file's MD5 sum.
	 */
	public static String getMD5Sum(String file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(file);
			byte[] buffer = new byte[8192];
			int read = 0;
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			is.close();
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			return bigInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

}

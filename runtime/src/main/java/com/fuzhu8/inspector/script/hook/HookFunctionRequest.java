package com.fuzhu8.inspector.script.hook;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XCallback;
import org.apache.commons.codec.binary.Hex;
import org.keplerproject.luajava.LuaObject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author zhkl0228
 *
 */
public abstract class HookFunctionRequest<T extends XCallback> {
	
	protected final String clazz, method;
	protected final LuaObject callback;
	protected final String[] params;
	
	protected HookFunctionRequest(String clazz, String method, LuaObject callback, String[] params) {
		super();
		this.clazz = clazz;
		this.method = method;
		this.callback = callback;
		this.params = params;
	}
	
	public final void tryHook(Class<?> class1, Inspector inspector, DexFileManager dexFileManager, Set<Member> hookedSet) throws ClassNotFoundException, NoSuchMethodException {
		ClassLoader classLoader = class1.getClassLoader();
		
		T callback = createCallback(inspector, dexFileManager);
		if("*".equals(method)) {
			for(Method method : class1.getDeclaredMethods()) {
				if (XposedBridge.isSynthetic(method.getModifiers()) ||
						Modifier.isAbstract(method.getModifiers())) {
					continue;
				}

				if(hookedSet.contains(method)) {
					continue;
				}
				hookedSet.add(method);
				executeHook(method, classLoader, callback, inspector);
			}
			return;
		}
		
		Class<?>[] paramClass = new Class<?>[params.length];
		for(int i = 0; i < params.length; i++) {
			paramClass[i] = findClass(classLoader, params[i]);
		}
		
		Member[] hookMethods = getHookMethods(class1, paramClass);
		for(Member hookMethod : hookMethods) {
			if (XposedBridge.isSynthetic(hookMethod.getModifiers()) ||
					Modifier.isAbstract(hookMethod.getModifiers())) {
				continue;
			}

			if(hookedSet.contains(hookMethod)) {
				continue;
			}
			hookedSet.add(hookMethod);
			executeHook(hookMethod, classLoader, callback, inspector);
		}
	}
	
	protected abstract T createCallback(Inspector inspector, DexFileManager dexFileManager);
	protected abstract void executeHook(Member hookMethod, ClassLoader classLoader, T callback, Inspector inspector);

	protected final Member[] getHookMethods(Class<?> class1, Class<?>[] paramClass) throws NoSuchMethodException {
		try {
			return method == null ? new Member[] {
				class1.getDeclaredConstructor(paramClass)
			} : new Member[] {
				class1.getDeclaredMethod(method, paramClass)
			};
		} catch(NoSuchMethodException e) {
			List<Member> list = new ArrayList<>();
			if(method == null) {
				list.addAll(Arrays.asList(class1.getDeclaredConstructors()));
			} else {
				for(Method method : class1.getDeclaredMethods()) {
					if(this.method.equals(method.getName())) {
						list.add(method);
					}
				}
			}
			if(list.isEmpty()) {
				throw e;
			}
			return list.toArray(new Member[0]);
		}
	}

	public static Class<?> findClass(ClassLoader classLoader, String className, int... dimensions) throws ClassNotFoundException {
		if(className.endsWith("[]")) {
			return findClass(classLoader, className.substring(0, className.length() - 2), new int[dimensions.length + 1]);
		}
		
		if("int".equalsIgnoreCase(className)) {
			return findClass(int.class, dimensions);
		}
		if("char".equalsIgnoreCase(className)) {
			return findClass(char.class, dimensions);
		}
		if("byte".equalsIgnoreCase(className)) {
			return findClass(byte.class, dimensions);
		}
		if("boolean".equalsIgnoreCase(className)) {
			return findClass(boolean.class, dimensions);
		}
		if("long".equalsIgnoreCase(className)) {
			return findClass(long.class, dimensions);
		}
		if("float".equalsIgnoreCase(className)) {
			return findClass(float.class, dimensions);
		}
		if("double".equalsIgnoreCase(className)) {
			return findClass(double.class, dimensions);
		}
		if("byte[]".equals(className)) {
			return byte[].class;
		}
		if("java.lang.String[]".equals(className)) {
			return String[].class;
		}
		if("java.lang.String_fuck".equals(className)) {
			return String.class;
		}
		
		return findClass(Class.forName(className, true, classLoader), dimensions);
	}

	public static Class<?> findClass(Class<?> clazz, int...dimensions) {
		if(dimensions == null || dimensions.length < 1) {
			return clazz;
		}
		
		return Array.newInstance(clazz, dimensions).getClass();
	}

	protected final void printHook(Member hookMethod, ClassLoader classLoader, Inspector inspector) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("hook ");
		buffer.append(hookMethod.getDeclaringClass().getCanonicalName());
		buffer.append("->");
		buffer.append((hookMethod instanceof Constructor) ? hookMethod.getDeclaringClass().getSimpleName() : hookMethod.getName());
		buffer.append('(');
		Class<?>[] paramType;
		if (hookMethod instanceof Constructor) {
			paramType = ((Constructor<?>) hookMethod).getParameterTypes();
		} else {
			paramType = ((Method) hookMethod).getParameterTypes();
		}
		if(paramType.length > 0) {
			for(Class<?> param : paramType) {
				buffer.append(param.getCanonicalName()).append(", ");
			}
			buffer.delete(buffer.length() - 2, buffer.length());
		}
		buffer.append(')');
		buffer.append(" from classloader ");
		buffer.append(classLoader);
		buffer.append(" successfully! ");
		inspector.println(buffer);
	}
	
	public static void afterHookedMethod(Inspector inspector, Member method, Object thisObject, Object result, Object ... args) {
		Class<?> clazz = method.getDeclaringClass();
		StringBuffer buffer = new StringBuffer();
		buffer.append(clazz.getName());
		if(thisObject != null) {
			buffer.append('[').append(thisObject).append(']');
		}
		buffer.append("->");
		buffer.append(method.getName());
		buffer.append('(');
		if(args != null &&
				args.length > 0) {
			for (Object arg : args) {
				buffer.append("\n  ");
				appendParam(buffer, arg, inspector);

				if (arg instanceof Throwable) {
					inspector.println(arg);
				}
			}
			buffer.delete(buffer.length() - 2, buffer.length());
			buffer.append('\n');
		}
		buffer.append(')');
		
		if(result != null) {
			buffer.append(" ret: ");
			
			appendParam(buffer, result, inspector);
			
			buffer.delete(buffer.length() - 2, buffer.length());
		}
		
		if(method instanceof Constructor && String.class == method.getDeclaringClass()) {
			buffer.append(" string: ").append(thisObject);
		}
		
		buffer.append(" [").append(method).append(']');
		
		inspector.println(buffer);
	}

	private static void appendParam(StringBuffer buffer, Object obj, Inspector inspector) {
		if(obj != null && obj.getClass().isArray()) {
			int len = Array.getLength(obj);
			if(len > 512 * 1024) {
				buffer.append(obj).append(", ");
				return;
			}
			
			if(obj instanceof byte[]) {
				byte[] data = (byte[]) obj;
				char[] hex = Hex.encodeHex(data);
				buffer.append(hex);
				
				inspector.inspect(data, new String(hex));
			}
			
			buffer.append('[');
			
			for(int i = 0; i < len; i++) {
				appendParam(buffer, Array.get(obj, i), inspector);
			}
			if(len > 0) {
				buffer.delete(buffer.length() - 2, buffer.length());
			}
			buffer.append("], ");
			return;
		}
		
		boolean isStr = obj instanceof String;
		if(isStr) {
			buffer.append('"');
		}
		buffer.append(obj);
		if(isStr) {
			buffer.append('"');
		}
		buffer.append(", ");
	}

}

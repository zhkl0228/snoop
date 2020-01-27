package de.robv.android.xposed.callbacks;

import java.lang.reflect.Member;

/**
 * @author zhkl0228
 *
 */
public interface XCMethodPointer<T, E> {
	
	Class<?> getDeclaringClass();
	Member getMethod();
	Class<?>[] getParameterTypes();
	
	T invoke(E thisObj, Object...args) throws Throwable;

}

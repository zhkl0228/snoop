/**
 * 
 */
package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;


/**
 * @author zhkl0228
 *
 */
public interface Hooker<T> {
	
	void hook(Class<?> clazz, final String method, Hookable handler, Class<?>...params) throws NoSuchMethodException;
	void hook(Class<?> clazz, final Member member, Hookable handler);

	void log(Object msg);
	
	void hookAllConstructors(Class<?> clazz, T callback);
	void hookMethod(Member method, T callback);

}

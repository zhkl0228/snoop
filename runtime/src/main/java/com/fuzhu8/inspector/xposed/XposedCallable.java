/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Member;
import java.util.Arrays;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.script.hook.AbstractCallable;

import de.robv.android.xposed.callbacks.XCMethodPointer;

/**
 * @author zhkl0228
 *
 */
public class XposedCallable extends AbstractCallable {
	
	private final XCMethodPointer<Object, Object> old;
	protected final Inspector inspector;

	public XposedCallable(XCMethodPointer<Object, Object> old, Inspector inspector) {
		super();
		this.old = old;
		this.inspector = inspector;
	}

	@Override
	public Object invoke(Object thisObj, Object... args) throws Throwable {
		if(inspector.isDebug()) {
			inspector.println("XposedCallable.invoke this=" + thisObj + ", invoke: " + Arrays.asList(args));
		}
		return old.invoke(thisObj, args);
	}

	@Override
	public Class<?> getDeclaringClass() {
		return old.getDeclaringClass();
	}

	@Override
	public Class<?>[] getParameterTypes() {
		return old.getParameterTypes();
	}

	@Override
	public Member getMethod() {
		throw new UnsupportedOperationException();
	}

}

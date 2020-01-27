/**
 * 
 */
package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCMethodPointer;

/**
 * @author zhkl0228
 *
 */
public abstract class XC_MethodHookAlteration<T, E> extends XC_MethodHook {

	@Override
	protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
		throw new UnsupportedOperationException();
	}

	@Override
	protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
		throw new UnsupportedOperationException();
	}
	
	protected abstract T invoked(XCMethodPointer<T, E> old, E thisObj, MethodHookParam param) throws Throwable;

}

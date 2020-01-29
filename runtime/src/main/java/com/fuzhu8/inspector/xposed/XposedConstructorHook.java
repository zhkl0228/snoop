package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.advisor.Hookable;
import de.robv.android.xposed.XC_ConstructorHook;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Constructor;

/**
 * @author zhkl0228
 *
 */
public class XposedConstructorHook extends XC_ConstructorHook {

	private final Hookable handler;

	public XposedConstructorHook(Hookable advisor) {
		super();
		this.handler = advisor;
	}

	@Override
	protected void beforeHookedConstructor(Constructor<?> constructor, Class<?> thisClass) {
		try {
			super.beforeHookedConstructor(constructor, thisClass);

			handler.handleBefore(constructor, thisClass, new Object[0]);
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

	@Override
	protected void afterHookedConstructor(Constructor<?> constructor, Object thisObject) {
		try {
			super.afterHookedConstructor(constructor, thisObject);

			handler.handleAfter(constructor, thisObject, new Object[0], null);
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

}

package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCallback;

import java.lang.reflect.Constructor;

public abstract class XC_ConstructorHook extends XCallback {

	public XC_ConstructorHook() {
		super();
	}
	public XC_ConstructorHook(int priority) {
		super(priority);
	}

	protected void beforeHookedConstructor(Constructor<?> constructor, Class<?> thisClass) throws Throwable {}

	protected void afterHookedConstructor(Constructor<?> constructor, Object thisObject) throws Throwable {}

}

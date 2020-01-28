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

	protected void beforeHookedConstructor(ConstructorBeforeHookParam param) throws Throwable {}

	protected void afterHookedConstructor(ConstructorAfterHookParam param) throws Throwable {}

	public static class ConstructorBeforeHookParam {

		public Constructor<?> constructor;
		public Class<?> thisClass;

	}

	public static class ConstructorAfterHookParam {

		public Constructor<?> constructor;
		public Object thisObject;

	}
}

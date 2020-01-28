package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCallback;

public abstract class XC_ClassInitializerHook extends XCallback {

	public XC_ClassInitializerHook() {
		super();
	}
	public XC_ClassInitializerHook(int priority) {
		super(priority);
	}

	protected void beforeHookedClassInitializer(ClassInitializerHookParam param) throws Throwable {}

	protected void afterHookedClassInitializer(ClassInitializerHookParam param) throws Throwable {}

	public static class ClassInitializerHookParam {

		public Class<?> thisClass;

	}
}

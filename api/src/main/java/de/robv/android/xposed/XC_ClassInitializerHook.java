package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCallback;

public abstract class XC_ClassInitializerHook extends XCallback {

	public XC_ClassInitializerHook() {
		super();
	}
	public XC_ClassInitializerHook(int priority) {
		super(priority);
	}

	protected void beforeHookedClassInitializer(Class<?> thisClass) throws Throwable {}

	protected void afterHookedClassInitializer(Class<?> thisClass) throws Throwable {}
}

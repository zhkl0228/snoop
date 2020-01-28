package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.advisor.Hookable;
import de.robv.android.xposed.XC_ConstructorHook;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Member;

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
	protected void beforeHookedConstructor(ConstructorBeforeHookParam param) {
		try {
			super.beforeHookedConstructor(param);

			Member hooked = param.constructor;

			handler.handleBefore(hooked, param.thisClass, new Object[0]);
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

	@Override
	protected void afterHookedConstructor(ConstructorAfterHookParam param) {
		try {
			super.afterHookedConstructor(param);

			Member hooked = param.constructor;

			handler.handleAfter(hooked, param.thisObject, new Object[0], null);
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

}

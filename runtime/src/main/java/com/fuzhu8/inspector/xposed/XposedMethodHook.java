/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import com.fuzhu8.inspector.advisor.Hookable;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class XposedMethodHook extends XC_MethodHook {
	
	private final Hookable handler;

	public XposedMethodHook(Hookable advisor) {
		super();
		this.handler = advisor;
	}

	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		try {
			super.beforeHookedMethod(param);
			
			Member hooked = param.method;
			
			Object value = handler.handleBefore(hooked, param.thisObject, param.args);
			if(value != null) {
				if(Method.class.isInstance(hooked) &&
						Method.class.cast(hooked).getReturnType() != void.class) {
					param.setResult(value);
				} else {
					param.setResult(null);
				}
			}
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		try {
			super.afterHookedMethod(param);
			
			if(param.hasThrowable()) {
				return;
			}
			
			Member hooked = param.method;
			
			Object result = handler.handleAfter(hooked, param.thisObject, param.args, param.getResult());
			if(Method.class.isInstance(hooked) &&
					Method.class.cast(hooked).getReturnType() != void.class) {
				param.setResult(result);
			}
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

}

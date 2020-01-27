/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Member;

import org.keplerproject.luajava.LuaObject;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
class XposedHookFunctionRequest extends HookFunctionRequest<XC_MethodHook> {
	
	/**
	 * 
	 * @param clazz
	 * @param method nil表示构造函数，*表示所有构造函数以及方法
	 * @param params
	 */
	XposedHookFunctionRequest(String clazz, String method, LuaObject callback, String...params) {
		super(clazz, method, callback, params);
	}

	@Override
	protected final XC_MethodHook createCallback(Inspector inspector, DexFileManager dexFileManager) {
		if(this.callback == null) {
			return new XposedHookHandler(inspector, dexFileManager);
		}
		
		return new XposedLuaCallback(inspector, this.callback);
	}

	@Override
	protected final void executeHook(Member hookMethod, ClassLoader classLoader, XC_MethodHook callback, Inspector inspector) {
		XposedBridge.hookMethod(hookMethod, callback);
		
		printHook(hookMethod, classLoader, inspector);
	}

}

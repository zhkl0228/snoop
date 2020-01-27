package com.fuzhu8.inspector.xposed;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.ClassLoaderListener;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunction;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class XposedHookFunction extends HookFunction implements ClassLoaderListener {

	public XposedHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector, dexFileManager);
	}

	@Override
	protected HookFunctionRequest<?> createHookFunctionRequest(String clazz, String method, LuaObject callback,
			String[] params) {
		return new XposedHookFunctionRequest(clazz, method, callback, params);
	}

	@Override
	protected void log(Throwable t) {
		XposedBridge.log(t);
	}

}

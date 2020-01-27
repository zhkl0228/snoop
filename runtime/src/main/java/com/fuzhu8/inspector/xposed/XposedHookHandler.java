/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class XposedHookHandler extends XC_MethodHook {
	
	private final Inspector inspector;
	protected final DexFileManager dexFileManager;

	public XposedHookHandler(Inspector inspector, DexFileManager dexFileManager) {
		super();
		this.inspector = inspector;
		this.dexFileManager = dexFileManager;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		super.afterHookedMethod(param);
		
		HookFunctionRequest.afterHookedMethod(inspector, param.method, param.thisObject, param.getResult(), param.args);
	}

}

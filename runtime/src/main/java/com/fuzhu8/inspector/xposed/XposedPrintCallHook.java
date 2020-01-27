/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.script.hook.HookFunctionRequest;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class XposedPrintCallHook extends XC_MethodHook {
	
	protected final Inspector inspector;
	private final boolean printInvoke;
	
	public XposedPrintCallHook(Inspector inspector, boolean printInvoke) {
		super();
		this.inspector = inspector;
		this.printInvoke = printInvoke;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		super.afterHookedMethod(param);
		
		if(!printInvoke) {
			return;
		}
		
		HookFunctionRequest.afterHookedMethod(inspector, param.method, param.thisObject, param.getResult(), param.args);
	}

}

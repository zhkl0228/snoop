/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.bytecode.AbstractDexFileManager;
import com.fuzhu8.inspector.bytecode.DexFileManager;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class XposedDexFileManager extends AbstractDexFileManager<XC_MethodHook> implements DexFileManager {

	public XposedDexFileManager(ModuleContext context) {
		super(context, new XposedHooker());
	}

	@Override
	protected XC_MethodHook createPrintCallHook() {
		return new XposedPrintCallHook(inspector, true);
	}

}

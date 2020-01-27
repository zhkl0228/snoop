/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.AbstractLuaScriptManager;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.script.hook.HookFunction;

/**
 * @author zhkl0228
 *
 */
public class XposedLuaScriptManager extends AbstractLuaScriptManager implements LuaScriptManager {

	public XposedLuaScriptManager(ModuleContext context) {
		super(context, new XposedHooker());
	}

	@Override
	protected HookFunction createHookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		return new XposedHookFunction(L, inspector, dexFileManager);
	}

}

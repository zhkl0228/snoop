/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractInspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class XposedInspector extends AbstractInspector implements Runnable, Inspector {

	public XposedInspector(ModuleContext context,
			DexFileManager dexFileManager, LuaScriptManager luaScriptManager, String mainClass, int pid) {
		super(context, dexFileManager, luaScriptManager, new XposedHooker(), mainClass, pid);
	}
	
	@Override
	protected void executeHook() {
		super.executeHook();
		
		XposedHelpers.findAndHookMethod(getClass(), "testHook1", int.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				
				println("testHook1 in=" + param.args[0]);
			}
		});
		
		XposedHelpers.findAndHookMethod(getClass(), "testHook2", int.class, new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				println("testHook2 in=" + param.args[0]);
				return 2;
			}
		});
		
		XposedHelpers.findAndHookMethod(getClass(), "testHook3", int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				
				println("testHook3 in=" + param.args[0] + ", ret=" + param.getResult());
			}
		});
	}

	public void test() {
		println(testHook1(5));
		println(testHook2(6));
		println(testHook3(7));
	}
	
	public static int testHook1(int in) {
		if(in <= 1) {
			return 1;
		}
		
		return in * testHook1(in - 1);
	}
	
	public static int testHook2(int in) {
		if(in <= 1) {
			return 1;
		}
		
		return in * testHook1(in - 1);
	}
	
	public static int testHook3(int in) {
		if(in <= 1) {
			return 1;
		}
		
		return in * testHook1(in - 1);
	}

}

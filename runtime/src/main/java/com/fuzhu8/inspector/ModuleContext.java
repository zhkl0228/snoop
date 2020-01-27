package com.fuzhu8.inspector;

import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * @author zhkl0228
 *
 */
public interface ModuleContext {
	
	ClassLoader getClassLoader();
	
	File getDataDir();
	
	File getModuleLibDir();
	
	Instrumentation getInstrumentation();

	void discoverPlugins(DexFileManager dexFileManager, Inspector inspector, LuaScriptManager scriptManager);

}

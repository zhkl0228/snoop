package com.aspect.snoop.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;

import com.fuzhu8.inspector.DefaultModuleContext;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.xposed.SSLTrustKiller;
import com.fuzhu8.inspector.xposed.XposedDexFileManager;
import com.fuzhu8.inspector.xposed.XposedInspector;
import com.fuzhu8.inspector.xposed.XposedLoadLibraryFake;
import com.fuzhu8.inspector.xposed.XposedLuaScriptManager;
import com.sun.jna.Native;
import com.sun.jna.Platform;

import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
class InspectorStarter {

	@SuppressWarnings("unused")
    static void startInspector(File tmpDir, String mainClass, int pid, String libraryPath, Instrumentation instrumentation) {
		if (!Platform.isMac()) {
			throw new IllegalStateException("Only support MacOSX");
		}

    	ModuleContext context = new DefaultModuleContext(tmpDir, SnoopAgent.class.getClassLoader(), new File(libraryPath), instrumentation);
    	new SSLTrustKiller(context);
    	
    	new XposedLoadLibraryFake(context);
		
		DexFileManager dexFileManager = new XposedDexFileManager(context);
		LuaScriptManager scriptManager = new XposedLuaScriptManager(context);
		
		Inspector inspector = new XposedInspector(context, dexFileManager, scriptManager, mainClass, pid);
		Native.getLastError();//初始化JNA
		Thread thread = new Thread(inspector, "InspectorListener_" + inspector.getListenerPort());
		thread.start();
		
		scriptManager.setInspector(inspector);
		dexFileManager.setInspector(inspector);
		
		try {
			scriptManager.registerAll(dexFileManager);
		} catch(Throwable t) {
			XposedBridge.log(t);
		}

		context.discoverPlugins(dexFileManager, inspector, scriptManager);
		inspector.println("Inspect process successfully! ");
	}

}

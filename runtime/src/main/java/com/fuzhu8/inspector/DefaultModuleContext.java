package com.fuzhu8.inspector;

import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;

import java.io.File;
import java.lang.instrument.Instrumentation;

/**
 * @author zhkl0228
 *
 */
public class DefaultModuleContext implements ModuleContext {
	
	private final File dataDir;
	private final ClassLoader classLoader;
	private final File libraryDir;
	private final Instrumentation instrumentation;

	public DefaultModuleContext(File tmpDir, ClassLoader classLoader,
			File libraryDir, Instrumentation instrumentation) {
		super();
		this.dataDir = tmpDir;
		this.classLoader = classLoader;
		this.libraryDir = libraryDir;
		this.instrumentation = instrumentation;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.ModuleContext#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.ModuleContext#getDataDir()
	 */
	@Override
	public File getDataDir() {
		return dataDir;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.ModuleContext#getModuleLibDir()
	 */
	@Override
	public File getModuleLibDir() {
		return libraryDir;
	}

	@Override
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	@Override
	public void discoverPlugins(DexFileManager dexFileManager, Inspector inspector, LuaScriptManager scriptManager) {
		File pluginDir = new File(libraryDir, "plugins");
		PluginSource pluginSource = PluginSources.jarSource(pluginDir, classLoader);
		PluginInitializer.initializePluginSource(pluginSource, instrumentation, inspector);
	}

}

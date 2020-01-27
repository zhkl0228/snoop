package com.fuzhu8.inspector;

import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.plugin.PluginClassFileTransformer;
import com.fuzhu8.inspector.script.LuaScriptManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
		File[] jars = pluginDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return "jar".equalsIgnoreCase(FilenameUtils.getExtension(name));
			}
		});
		if (jars != null) {
			for (File jar : jars) {
				try {
					discoverPlugin(inspector, jar);
				} catch (Exception ignored) {
				}
			}
		}
	}

	private void discoverPlugin(Inspector inspector, File jar) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		try (JarFile jarFile = new JarFile(jar)) {
			JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
			if (entry == null) {
				return;
			}
			try (InputStream inputStream = jarFile.getInputStream(entry)) {
				for (String line : IOUtils.readLines(inputStream, StandardCharsets.UTF_8)) {
					if (line.startsWith("Snoop-Plugin:")) {
						String pluginClassName = line.substring(13).trim();
						URLClassLoader loader = new URLClassLoader(new URL[] {
								jar.toURI().toURL()
						}, classLoader);
						Class<?> pluginClass = loader.loadClass(pluginClassName);
						Constructor<?> constructor = pluginClass.getDeclaredConstructor(Inspector.class);
						constructor.setAccessible(true);
						Plugin plugin = (Plugin) constructor.newInstance(inspector);
						instrumentation.addTransformer(new PluginClassFileTransformer(inspector, plugin));
						inspector.println("Discover plugins: " + plugin);
					}
				}
			}
		}
	}

}

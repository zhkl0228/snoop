/**
 * 
 */
package com.fuzhu8.inspector.bytecode;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public interface DexFileManager {
	
	/**
	 * 内存
	 * @param startAddr
	 * @param length
	 * @return
	 */
	ByteBuffer dumpMemory(long startAddr, long length);
	
	void addClassLoaderListener(ClassLoaderListener listener);
	
	String[] requestHookClasses(String classFilter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

	/**
	 * 组装
	 * @param inspector
	 */
	void setInspector(Inspector inspector);
	
	/**
	 * 发现ClassLoader
	 */
	void discoverClassLoader();
	
	List<Class> getLoadedClasses();
	
	/**
	 * 获取class bytecode
	 * @param clazz
	 * @return bytecode
	 */
	byte[] getClassBytes(String clazz);

}

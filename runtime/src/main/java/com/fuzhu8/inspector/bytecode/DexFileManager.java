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
	 */
	ByteBuffer dumpMemory(long startAddr, long length);
	
	void addClassLoaderListener(ClassLoaderListener listener);
	
	String[] requestHookClasses(String classFilter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

	/**
	 * 组装
	 */
	void setInspector(Inspector inspector);
	
	List<Class<?>> getLoadedClasses();
	
	/**
	 * 获取class bytecode
	 * @return bytecode
	 */
	byte[] getClassBytes(String clazz);

}

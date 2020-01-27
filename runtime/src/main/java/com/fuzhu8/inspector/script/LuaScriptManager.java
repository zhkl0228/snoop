/**
 * 
 */
package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.LuaException;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;

/**
 * @author zhkl0228
 *
 */
public interface LuaScriptManager {
	
	/**
	 * 注册函数
	 * @param name
	 * @param function
	 * @throws LuaException
	 */
	void registerFunction(String name, InspectorFunction function) throws LuaException;

	/**
	 * 执行lua脚本
	 * @param lua
	 * @throws LuaException
	 */
	void eval(String lua) throws LuaException;

	/**
	 * 注册所有函数
	 * @param dexFileManager
	 * @throws LuaException
	 */
	void registerAll(DexFileManager dexFileManager) throws LuaException;

	/**
	 * 组装
	 * @param inspector
	 */
	void setInspector(Inspector inspector);
	
	/**
	 * 注册全局变量
	 * @param name
	 * @param obj
	 * @throws LuaException
	 */
	void registerGlobalObject(String name, Object obj) throws LuaException;

}

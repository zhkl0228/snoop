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
	 */
	void registerFunction(String name, InspectorFunction function) throws LuaException;

	/**
	 * 执行lua脚本
	 */
	void eval(String lua) throws LuaException;

	/**
	 * 注册所有函数
	 */
	void registerAll(DexFileManager dexFileManager) throws LuaException;

	/**
	 * 组装
	 */
	void setInspector(Inspector inspector);
	
	/**
	 * 注册全局变量
	 */
	void registerGlobalObject(String name, Object obj) throws LuaException;

}

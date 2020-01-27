package com.fuzhu8.inspector;

import java.io.File;
import java.nio.ByteBuffer;

import com.fuzhu8.inspector.completer.ServerCommandCompleter;
import com.fuzhu8.inspector.io.InspectCache;

public interface Inspector extends Runnable {

	void println(Object msg);
	
	void inspect(byte[] data, String label);
	
	void inspect(ByteBuffer data, String label);
	
	void writeToConsole(InspectCache cache);
	
	void inspect(byte[] data, boolean send);
	
	void inspect(int type, byte[] data, boolean send);
	
	void evalLuaScript(String script);
	
	void inspect(short[] data, String label);
	
	void setDebug(boolean debug);
	boolean isDebug();
	
	/**
	 * 应用dataDir
	 */
	File getAppDataDir();
	
	/**
	 * 模块lib目录
	 */
	File getModuleLibDir();
	
	int getListenerPort();
	
	/**
	 * 创建一个命令完成器
	 * @return 返回的对象用commit提交
	 */
	ServerCommandCompleter createCommandCompleter(String prefix);

}
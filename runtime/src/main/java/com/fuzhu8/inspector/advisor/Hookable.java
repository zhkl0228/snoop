/**
 * 
 */
package com.fuzhu8.inspector.advisor;

import java.io.File;
import java.lang.reflect.Member;

/**
 * @author zhkl0228
 *
 */
public interface Hookable extends HookOperation {
	
	Object handleBefore(Member hooked, Object thisObj, Object[] args);
	
	Object handleAfter(Member hooked, Object thisObj, Object[] args, Object ret);
	
	/**
	 * 应用dataDir
	 * @return
	 */
	File getAppDataDir();
	
	/**
	 * 模块lib目录
	 * @return
	 */
	File getModuleLibDir();
	
	void log(Object msg);

}

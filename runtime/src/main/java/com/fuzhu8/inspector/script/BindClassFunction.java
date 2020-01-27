/**
 * 
 */
package com.fuzhu8.inspector.script;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;

/**
 * @author zhkl0228
 *
 */
public class BindClassFunction extends InspectorFunction {
	
	private final DexFileManager dexFileManager;

	public BindClassFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() throws LuaException {
		if(L.getTop() > 1) {
			String className = getParam(2).getString();
			for(Class<?> cls : dexFileManager.getLoadedClasses()) {
				if(!className.equals(cls.getCanonicalName())) {
					continue;
				}
				
				L.pushObjectValue(cls);
				return 1;
			}
		}
		return 0;
	}

}

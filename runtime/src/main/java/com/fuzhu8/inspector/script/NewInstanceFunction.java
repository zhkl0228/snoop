/**
 * 
 */
package com.fuzhu8.inspector.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.DexFileManager;

/**
 * @author zhkl0228
 *
 */
public class NewInstanceFunction extends InspectorFunction {
	
	private final DexFileManager dexFileManager;

	public NewInstanceFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public int execute() throws LuaException {
		int count = L.getTop();
		if(count >= 2) {
			String className = getParam(2).getString();
			LuaObject[] params = new LuaObject[count - 2];
			for(int i = 0; i < params.length; i++) {
				params[i] = getParam(3 + i);
			}
			
			for(Class<?> cls : dexFileManager.getLoadedClasses()) {
				if(!className.equals(cls.getCanonicalName())) {
					continue;
				}
				
				try {
					Object obj = newInstance(className, params, cls);
					if(obj != null) {
						L.pushObjectValue(obj);
						return 1;
					}
				} catch(Exception e) {
					inspector.println(e);
					return 0;
				}
			}
		}
		return 0;
	}

	private Object newInstance(String className, LuaObject[] params, Class<?> clazz) throws ClassNotFoundException, LuaException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for(Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			Class<?>[] types = constructor.getParameterTypes();
			if(types.length != params.length) {
				continue;
			}

			Object[] args = new Object[types.length];
			boolean valid = true;
			for(int i = 0; i < types.length; i++) {
				Class<?> c = types[i];
				if(c == String.class && params[i].isString()) {
					args[i] = params[i].getString();
					continue;
				}
				
				if((c == boolean.class || c == Boolean.class) && params[i].isBoolean()) {
					args[i] = params[i].getBoolean();
					continue;
				}
				
				if(Number.class.isAssignableFrom(c) && params[i].isNumber()) {
					args[i] = LuaState.convertLuaNumber(params[i].getNumber(), c);
					continue;
				}
				
				if(params[i].isNil()) {
					args[i] = null;
					continue;
				}
				
				if(!params[i].isJavaObject()) {
					valid = false;
					break;
				}
				
				args[i] = params[i].getObject();
			}
			if(!valid) {
				continue;
			}
			
			constructor.setAccessible(true);
			return constructor.newInstance(args);
		}
		return null;
	}

}

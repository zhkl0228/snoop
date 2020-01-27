package com.fuzhu8.inspector.script.hook;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.bytecode.ClassLoaderListener;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.script.InspectorFunction;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhkl0228
 *
 */
public abstract class HookFunction extends InspectorFunction implements ClassLoaderListener {
	
	private final DexFileManager dexFileManager;
	private final Set<Member> hookedSet = new HashSet<Member>();

	public HookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
	}

	/* (non-Javadoc)
	 * @see org.keplerproject.luajava.JavaFunction#execute()
	 */
	@Override
	public final int execute() {
		int count = L.getTop();
		if(count >= 3) {
			String clazz = getParam(2).getString();
			String method = getParam(3).getString();
			LuaObject[] params = new LuaObject[count - 3];
			for(int i = 0; i < params.length; i++) {
				params[i] = getParam(4 + i);
			}
			
			LuaObject callback = null;
			String[] types;
			if(params.length > 0 && params[params.length - 1].isFunction()) {
				callback = params[params.length - 1];
				types = new String[params.length - 1];
			} else {
				types = new String[params.length];
			}
			for(int i = 0; i < types.length; i++) {
				types[i] = params[i].getString();
			}
			
			executeHook(clazz, method, callback, types);
		}
		return 0;
	}

	private void executeHook(String clazz, String method, LuaObject callback, String...params) {
		HookFunctionRequest<?> request = createHookFunctionRequest(clazz, method, callback, params);
		if (inspector.isDebug()) {
			inspector.println("executeHook className=" + clazz + ", method=" + method);
		}
		for(Class<?> cls : dexFileManager.getLoadedClasses()) {
			if(!clazz.equals(cls.getCanonicalName())) {
				continue;
			}
			try {
				if (inspector.isDebug()) {
					inspector.println("Try hook class=" + cls);
				}
				request.tryHook(cls, inspector, dexFileManager, hookedSet);
			} catch(Exception t) {
				log(t);
				// inspector.println("hook from classloader " + dex.getClassLoader() + " failed: " + t.getMessage());
			}
		}
	}

	protected abstract HookFunctionRequest<?> createHookFunctionRequest(String clazz, String method, LuaObject callback,
			String[] params);

	@Override
	public final void notifyClassLoader(ClassLoader classLoader) {
		throw new UnsupportedOperationException();
	}
	
	protected abstract void log(Throwable t);

}

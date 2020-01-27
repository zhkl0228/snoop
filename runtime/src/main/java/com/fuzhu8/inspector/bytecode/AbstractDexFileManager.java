/**
 * 
 */
package com.fuzhu8.inspector.bytecode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractAdvisor;
import com.fuzhu8.inspector.advisor.Hooker;
import com.sun.jna.Pointer;

import cn.banny.utils.StringUtils;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import javassist.ClassPool;
import javassist.CtClass;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractDexFileManager<T> extends AbstractAdvisor implements
		DexFileManager {

	public AbstractDexFileManager(ModuleContext context, Hooker<T> hooker) {
		super(context, hooker);
	}
	
	protected Inspector inspector;

	@Override
	public void setInspector(Inspector inspector) {
		this.inspector = inspector;
	}
	
	private final Set<ClassLoader> discoveredClassLoaders = new LinkedHashSet<ClassLoader>();

	@Override
	public final void discoverClassLoader() {
		try {
			Class<?> VMStack = Class.forName("dalvik.system.VMStack");
			Class<?>[] classes = (Class<?>[]) XposedHelpers.callStaticMethod(VMStack, "getClasses", -1);
			for(Class<?> clazz : classes) {
				ClassLoader classLoader = clazz.getClassLoader();
				if(discoveredClassLoaders.add(classLoader)) {
					inspector.println("discoverClassLoader: " + classLoader);
					notifyClassLoader(classLoader);
				}
			}
		} catch(ClassNotFoundException e) {
			inspector.println(e);
		}
	}

	@Override
	public String[] requestHookClasses(String classFilter) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Set<String> hooked = new LinkedHashSet<String>();
		for(Class<?> clazz : context.getInstrumentation().getAllLoadedClasses()) {
			try {
				String name = clazz.getCanonicalName();
				if(StringUtils.isEmpty(name)) {
					continue;
				}
				
				if(classFilter != null && !name.contains(classFilter)) {
					continue;
				}
				
				hookAllMember(clazz, inspector);
				hooked.add(name);
			} catch(Exception t) {
				inspector.println(t);
			}
		}
		
		return hooked.toArray(new String[0]);
	}
	
	protected abstract T createPrintCallHook();

	protected final void hookAllMember(Class<?> clazz, Inspector inspector) {
		T dumpCallback = createPrintCallHook();

		for(Method method : clazz.getDeclaredMethods()) {
			if(Modifier.isAbstract(method.getModifiers())) {
				continue;
			}
			if(Modifier.isNative(method.getModifiers())) {
				continue;
			}
			if(XposedBridge.isSynthetic(method.getModifiers())) {
				continue;
			}
			
			if("toString".equals(method.getName())) {
				continue;
			}
			
			hooker.hookMethod(method, dumpCallback);
		}
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexFileManager#dumpMemory(long, long)
	 */
	@Override
	public ByteBuffer dumpMemory(long startAddr, long length) {
		if(startAddr < 1) {
			throw new IllegalArgumentException("dumpMemory startAddr=" + startAddr);
		}
		
		return new Pointer(startAddr).getByteBuffer(0, length);
	}

	/**
	 * 此处会引起基地新版本退出
	 * @see com.fuzhu8.inspector.advisor.AbstractAdvisor#executeHook()
	 */
	@Override
	protected void executeHook() {
	}
	
	protected boolean canHookDefineClass() {
		return true;
	}
	
	private final List<ClassLoaderListener> listeners = new ArrayList<ClassLoaderListener>();
	
	@Override
	public void addClassLoaderListener(ClassLoaderListener listener) {
		this.listeners.add(listener);
	}
	
	private void notifyClassLoader(ClassLoader classLoader) {
		for(ClassLoaderListener listener : this.listeners) {
			listener.notifyClassLoader(classLoader);
		}
	}

    public List<Class> getLoadedClasses() {
        List<Class> classes = new ArrayList<Class>();
        for ( Class c  : context.getInstrumentation().getAllLoadedClasses() ) {
            if ( ! c.isArray() && ! c.isPrimitive() && ! c.isSynthetic() ) {
                classes.add( c );
            }
        }
        return classes;
    }

	@Override
	public byte[] getClassBytes(String clazz) {
		try {
            CtClass cls = ClassPool.getDefault().get(clazz);    
            return cls.toBytecode();
        } catch (Exception ex) {
            inspector.println(ex);
        }
        return null;
	}

}

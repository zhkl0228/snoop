/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaJavaAPI;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractHookHandler;
import com.sun.jna.Platform;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class XposedLoadLibraryFake extends AbstractHookHandler {

	public XposedLoadLibraryFake(final ModuleContext context) {
		super(context, new XposedHooker());

		try {
			XposedHelpers.findAndHookMethod(ClassLoader.class, "loadLibrary", Class.class, String.class, boolean.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);
					
					Class<?> fromClass = (Class<?>) param.args[0];
					String name = (String) param.args[1];
					boolean isAbsolute = (Boolean) param.args[2];

					// log("loadLibrary this=" + param.thisObject + ", fromClass=" + fromClass + ", name=" + name + ", isAbsolute=" + isAbsolute);
					
					if("luajava-1.1".equals(name)) {
						loadLibrary(param, fromClass, System.mapLibraryName(name));
					} else if("jnidispatch".equals(name)) {
						loadLibrary(param, fromClass, "com/sun/jna/" + Platform.RESOURCE_PREFIX + "/" + System.mapLibraryName(name).replace(".dylib", ".jnilib"));
					}
				}
			});
			
			XposedHelpers.findAndHookMethod(LuaJavaAPI.class, "checkField", int.class, Object.class, String.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					int luaState = (Integer) param.args[0];
					Object obj = param.args[1];
					String fieldName = (String) param.args[2];
					return checkField(luaState, obj, fieldName);
				}
			});
			
			XposedHelpers.findAndHookMethod(LuaJavaAPI.class, "checkMethod", int.class, Object.class, String.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					int luaState = (Integer) param.args[0];
					Object obj = param.args[1];
					String methodName = (String) param.args[2];
					return checkMethod(luaState, obj, methodName);
				}
			});
			
			XposedHelpers.findAndHookMethod(LuaJavaAPI.class, "objectIndex", int.class, Object.class, String.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					int luaState = (Integer) param.args[0];
					Object obj = param.args[1];
					String methodName = (String) param.args[2];
					return objectIndex(luaState, obj, methodName);
				}
			});
			
			XposedHelpers.findAndHookMethod(LuaState.class, "convertLuaNumber", Double.class, Class.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					Double db = (Double) param.args[0];
					Class retType = (Class) param.args[1];
					Object ret = convertLuaNumber(db, retType);
					// System.err.println("db=" + db + ", retType=" + retType + ", ret=" + ret + ", retClass=" + ret.getClass());
					return ret;
				}
			});
		} catch (Exception e) {
			log(e);
		}
	}

	  private static boolean isXC_invoked(Class<?> clazz, String methodName) {
		  Class<?> XCMethodPointer = de.robv.android.xposed.callbacks.XCMethodPointer.class;
		  if(XCMethodPointer.isAssignableFrom(clazz)) {
			  return true;
		  }
		  
		  if(!"invoke".equals(methodName) && !"call".equals(methodName)) {
			  return false;
		  }
		  
		  try {
			  XCMethodPointer = clazz.getClassLoader().loadClass("de.robv.android.xposed.callbacks.XCMethodPointer");
			  return XCMethodPointer.isAssignableFrom(clazz);
		  } catch(ClassNotFoundException e) {
			  return false;
		  }
	}

	  private static Object compareTypes(LuaState L, Class parameter, int idx)
	    throws LuaException
	  {
	    boolean okType = true;
	    Object obj = null;

	    if (L.isBoolean(idx))
	    {
	      if (parameter.isPrimitive())
	      {
	        if (parameter != Boolean.TYPE)
	        {
	          okType = false;
	        }
	      }
	      else if (!parameter.isAssignableFrom(Boolean.class))
	      {
	        okType = false;
	      }
	      obj = Boolean.valueOf(L.toBoolean(idx));
	    }
	    else if (L.type(idx) == LuaState.LUA_TSTRING.intValue())
	    {
	    	if(parameter == Long.class || parameter == long.class) {
	    		obj = Long.parseLong(L.toString(idx)); // 兼容64位long，用字符串代替
	    	} else {
		    	
	  	      if (!parameter.isAssignableFrom(String.class))
	  	      {
	  	        okType = false;
	  	      }
	  	      else
	  	      {
	  	        obj = L.toString(idx);
	  	      }
	    	}
	    }
	    else if (L.isFunction(idx))
	    {
	      if (!parameter.isAssignableFrom(LuaObject.class))
	      {
	        okType = false;
	      }
	      else
	      {
	        obj = L.getLuaObject(idx);
	      }
	    }
	    else if (L.isTable(idx))
	    {
	      if (!parameter.isAssignableFrom(LuaObject.class))
	      {
	        okType = false;
	      }
	      else
	      {
	        obj = L.getLuaObject(idx);
	      }
	    }
	    else if (L.type(idx) == LuaState.LUA_TNUMBER.intValue())
	    {
	      Double db = Double.valueOf(L.toNumber(idx));
	      
	      obj = LuaState.convertLuaNumber(db, parameter);
	      if (obj == null)
	      {
	        okType = false;
	      }
	    }
	    else if (L.isUserdata(idx))
	    {
	      if (L.isObject(idx))
	      {
	        Object userObj = L.getObjectFromUserdata(idx);
	        if (!parameter.isAssignableFrom(userObj.getClass()))
	        {
	          okType = false;
	        }
	        else
	        {
	          obj = userObj;
	        }
	      }
	      else
	      {
	        if (!parameter.isAssignableFrom(LuaObject.class))
	        {
	          okType = false;
	        }
	        else
	        {
	          obj = L.getLuaObject(idx);
	        }
	      }
	    }
	    else if (L.isNil(idx))
	    {
	      obj = null;
	    }
	    else
	    {
	      throw new LuaException("Invalid Parameters.");
	    }

	    if (!okType)
	    {
	      throw new LuaException("Invalid Parameter.");
	    }

	    return obj;
	  }
	
	private void loadLibrary(MethodHookParam param, Class<?> fromClass, String filename) {
		File file = new File(context.getModuleLibDir(), filename);
		loadLibrary0(fromClass, file, param);
	}

	private void loadLibrary0(Class<?> fromClass, File file, MethodHookParam param) {
		boolean b = (Boolean) XposedHelpers.callStaticMethod(ClassLoader.class, "loadLibrary0", fromClass, file);
		if (b) {
			param.setResult(null);
            return;
        }
        param.setThrowable(new UnsatisfiedLinkError("Can't load " + file.getName()));
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.advisor.AbstractHookHandler#getHandler()
	 */
	@Override
	protected Object getHandler() {
		return this;
	}

	  /**
	   * Checks if there is a field on the obj with the given name
	   * 
	   * @param luaState int that represents the state to be used
	   * @param obj object to be inspected
	   * @param fieldName name of the field to be inpected
	   * @return number of returned objects
	   */
	  private static int checkField(int luaState, Object obj, String fieldName)
	  	throws LuaException
	  {
	    LuaState L = LuaStateFactory.getExistingState(luaState);

	    synchronized (L)
	    {
	      Field field = null;
	      Class objClass;

	      if (obj instanceof Class)
	      {
	        objClass = (Class) obj;
	      }
	      else
	      {
	        objClass = obj.getClass();
	      }
	      
	      Field[] fields = objClass.getFields();
	      for(Field f : fields) {
	    	  if(f.getName().equals(fieldName)) {
	    		  field = f;
	    		  break;
	    	  }
	      }
	      
	      if(field == null) {
	    	  fields = objClass.getDeclaredFields();
	    	  for(Field f : fields) {
	    		  if(f.getName().equals(fieldName)) {
	    			  f.setAccessible(true);
	    			  field = f;
	    			  break;
	    		  }
	    	  }
	      }

	      if (field == null)
	      {
	        return 0;
	      }

	      Object ret = null;
	      try
	      {
	        ret = field.get(obj);
	      }
	      catch (Exception e1)
	      {
	        return 0;
	      }

	      L.pushObjectValue(ret);

	      return 1;
	    }
	  }

	  /**
	   * Checks to see if there is a method with the given name.
	   * 
	   * @param luaState int that represents the state to be used
	   * @param obj object to be inspected
	   * @param methodName name of the field to be inpected
	   * @return number of returned objects
	   */
	  private static int checkMethod(int luaState, Object obj, String methodName)
	  {
	    LuaState L = LuaStateFactory.getExistingState(luaState);

	    synchronized (L)
	    {
	      Class clazz;

	      if (obj instanceof Class)
	      {
	        clazz = (Class) obj;
	      }
	      else
	      {
	        clazz = obj.getClass();
	      }

	      Method[] methods = clazz.getMethods();

	      for (int i = 0; i < methods.length; i++)
	      {
	        if (methods[i].getName().equals(methodName))
	          return 1;
	      }
	      
	      methods = clazz.getDeclaredMethods();
	      for(Method method : methods) {
	    	  if(method.getName().equals(methodName)) {
	    		  method.setAccessible(true);
	    		  return 1;
	    	  }
	      }

	      return 0;
	    }
	  }

	  /**
	   * Java implementation of the metamethod __index
	   * 
	   * @param luaState int that indicates the state used
	   * @param obj Object to be indexed
	   * @param methodName the name of the method
	   * @return number of returned objects
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	   */
	  public static int objectIndex(int luaState, Object obj, String methodName)
	      throws LuaException, IllegalAccessException, InvocationTargetException
	  {
	    LuaState L = LuaStateFactory.getExistingState(luaState);

	    synchronized (L)
	    {
	      int top = L.getTop();

	      Object[] objs = new Object[top - 1];

	      Class clazz;

	      if (obj instanceof Class)
	      {
	        clazz = (Class) obj;
	      }
	      else
	      {
	        clazz = obj.getClass();
	      }

	      
	      boolean isXC_invoked = isXC_invoked(clazz, methodName);
	      if(isXC_invoked) {
	    	  objs = new Object[2];
	      }
	      
	      Method method = findMethod(clazz, methodName, L, top, isXC_invoked, objs, obj);

	      // If method is null means there isn't one receiving the given arguments
	      if (method == null)
	      {
	        throw new LuaException("Invalid method call. No such method: obj=" + obj + ", methodName=" + methodName);
	      }

	      Object ret;
	      if(!Modifier.isPublic(method.getModifiers()))
	      {
	        method.setAccessible(true);
	      }
	      
	      if (obj instanceof Class)
	      {
	        ret = method.invoke(null, objs);
	      }
	      else
	      {
	        ret = method.invoke(obj, objs);
	      }

	      // Void function returns null
	      if (ret == null)
	      {
	        return 0;
	      }

	      // push result
	      L.pushObjectValue(ret);

	      return 1;
	    }
	  }

		private static Method findMethod(Class clazz, String methodName, LuaState L, int top, boolean isXC_invoked, Object[] objs, Object obj) {
			Method method = null;
			Method[] methods = clazz.getDeclaredMethods();

		      // gets method and arguments
		      for (int i = 0; i < methods.length; i++)
		      {
		        if (!methods[i].getName().equals(methodName))
		          continue;
		        
		        if(isXC_invoked) {
		        	Class[] parameters = (Class[]) XposedHelpers.callMethod(obj, "getParameterTypes");
		            if (parameters.length != top - 2) {
		              continue;
		            }
		            
		            boolean okMethod = true;
		            try {
		            	objs[0] = compareTypes(L, (Class) XposedHelpers.callMethod(obj, "getDeclaringClass"), 2);
		            } catch(Exception e) {
		            	okMethod = false;
		            	break;
		            }
		            Object[] args = new Object[parameters.length];
		            for (int j = 0; j < parameters.length; j++) {
		              try {
		            	 args[j] = compareTypes(L, parameters[j], j + 3);
		              } catch (Exception e) {
		                okMethod = false;
		                break;
		              }
		            }

		            if (okMethod) {
		              objs[1] = args;
		              method = methods[i];
		              break;
		            }
		            continue;
		        }

		        Class[] parameters = methods[i].getParameterTypes();
		        if (parameters.length != top - 1)
		          continue;

		        boolean okMethod = true;

		        for (int j = 0; j < parameters.length; j++)
		        {
		          try
		          {
		            objs[j] = compareTypes(L, parameters[j], j + 2);
		          }
		          catch (Exception e)
		          {
		            okMethod = false;
		            break;
		          }
		        }

		        if (okMethod)
		        {
		          method = methods[i];
		          break;
		        }

		      }
		      
		      if(method != null) {
		    	  return method;
		      }
		      
		      Class<?> parentClass = clazz.getSuperclass();
		      return parentClass == null ? method : findMethod(parentClass, methodName, L, top, isXC_invoked, objs, obj);
		}

	/**
	 * When you call a function in lua, it may return a number, and the
	 * number will be interpreted as a <code>Double</code>.<br>
	 * This function converts the number into a type specified by 
	 * <code>retType</code>
	 * @param db lua number to be converted
	 * @param retType type to convert to
	 * @return The converted number
	 */
	public static Number convertLuaNumber(Double db, Class retType)
	{
	  // checks if retType is a primitive type
    if (retType.isPrimitive())
    {
      if (retType == Integer.TYPE)
      {
        return Integer.valueOf(db.intValue());
      }
      else if (retType == Long.TYPE)
      {
        return Long.valueOf(db.longValue());
      }
      else if (retType == Float.TYPE)
      {
        return Float.valueOf(db.floatValue());
      }
      else if (retType == Double.TYPE)
      {
        return db;
      }
      else if (retType == Byte.TYPE)
      {
        return Byte.valueOf(db.byteValue());
      }
      else if (retType == Short.TYPE)
      {
        return Short.valueOf(db.shortValue());
      }
    }
    else if (retType.isAssignableFrom(Number.class))
    {
      // Checks all possibilities of number types
      if (retType.isAssignableFrom(Integer.class))
      {
        return Integer.valueOf(db.intValue());
      }
      else if (retType.isAssignableFrom(Long.class))
      {
        return Long.valueOf(db.longValue());
      }
      else if (retType.isAssignableFrom(Float.class))
      {
        return Float.valueOf(db.floatValue());
      }
      else if (retType.isAssignableFrom(Double.class))
      {
        return db;
      }
      else if (retType.isAssignableFrom(Byte.class))
      {
        return Byte.valueOf(db.byteValue());
      }
      else if (retType.isAssignableFrom(Short.class))
      {
        return Short.valueOf(db.shortValue());
      }
    }
    
    // if all checks fail, return null
    return null;	  
	}

}

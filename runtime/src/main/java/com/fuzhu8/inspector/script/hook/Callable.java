/**
 * 
 */
package com.fuzhu8.inspector.script.hook;

import de.robv.android.xposed.callbacks.XCMethodPointer;

/**
 * @author zhkl0228
 *
 */
public interface Callable<T, E> extends XCMethodPointer<T, E> {
	
	Object getOriginal();

}

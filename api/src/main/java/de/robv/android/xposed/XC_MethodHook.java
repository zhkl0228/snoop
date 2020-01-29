package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCallback;

import java.lang.reflect.Method;

public abstract class XC_MethodHook extends XCallback {

	public XC_MethodHook() {
		super();
	}
	public XC_MethodHook(int priority) {
		super(priority);
	}

	/**
	 * Called before the invocation of the method.
	 * <p>Can use {@link MethodHookParam#setResult(Object)} and {@link MethodHookParam#setThrowable(Throwable)}
	 * to prevent the original method from being called.
	 */
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

	/**
	 * Called after the invocation of the method.
	 * <p>Can use {@link MethodHookParam#setResult(Object)} and {@link MethodHookParam#setThrowable(Throwable)}
	 * to modify the return value of the original method.
	 */
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

	public static class MethodHookParam {
		/** Description of the hooked method */
		public Method method;
		/** The <code>this</code> reference for an instance method, or null for static methods */
		public Object thisObject;
		/** Arguments to the method call */
		public Object[] args;

		private Object result = null;
		private Throwable throwable = null;
		/* package */ boolean returnEarly = false;

		/** Returns the result of the method call */
		public Object getResult() {
			return result;
		}

		/**
		 * Modify the result of the method call. In a "before-method-call"
		 * hook, prevents the call to the original method.
		 * You still need to "return" from the hook handler if required.
		 */
		public void setResult(Object result) {
			this.result = result;
			this.throwable = null;
			this.returnEarly = true;
		}

		/** Returns the <code>Throwable</code> thrown by the method, or null */
		public Throwable getThrowable() {
			return throwable;
		}

		/** Returns true if an exception was thrown by the method */
		public boolean hasThrowable() {
			return throwable != null;
		}

		/**
		 * Modify the exception thrown of the method call. In a "before-method-call"
		 * hook, prevents the call to the original method.
		 * You still need to "return" from the hook handler if required.
		 */
		public void setThrowable(Throwable throwable) {
			this.throwable = throwable;
			this.result = null;
			this.returnEarly = true;
		}
	}
}

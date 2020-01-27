package de.robv.android.xposed.callbacks;

import java.io.Serializable;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;

public abstract class XCallback implements Comparable<XCallback> {
	public final int priority;
	public XCallback() {
		this.priority = PRIORITY_DEFAULT;
	}
	public XCallback(int priority) {
		this.priority = priority;
	}

	public static class Param {
		public final Object[] callbacks;

		protected Param() {
			callbacks = null;
		}

		protected Param(CopyOnWriteSortedSet<? extends XCallback> callbacks) {
			this.callbacks = callbacks.getSnapshot();
		}

		private static class SerializeWrapper implements Serializable {
			private static final long serialVersionUID = 1L;
			private Object object;
			public SerializeWrapper(Object o) {
				object = o;
			}
		}
	}

	public static void callAll(Param param) {
		if (param.callbacks == null)
			throw new IllegalStateException("This object was not created for use with callAll");

		for (int i = 0; i < param.callbacks.length; i++) {
			try {
				((XCallback) param.callbacks[i]).call(param);
			} catch (Throwable t) { XposedBridge.log(t); }
		}
	}

	protected void call(Param param) throws Throwable {};

	@Override
	public int compareTo(XCallback other) {
		if (this == other)
			return 0;

		// order descending by priority
		if (other.priority != this.priority)
			return other.priority - this.priority;
		// then randomly
		else if (System.identityHashCode(this) < System.identityHashCode(other))
			return -1;
		else
			return 1;
	}

	public static final int PRIORITY_DEFAULT = 50;
	/** Call this handler last */
	public static final int PRIORITY_LOWEST = -10000;
	/** Call this handler first */
	public static final int PRIORITY_HIGHEST = 10000;
}

package de.robv.android.xposed.callbacks;

public abstract class XCallback implements Comparable<XCallback> {

	public final int priority;
	public XCallback() {
		this.priority = PRIORITY_DEFAULT;
	}
	public XCallback(int priority) {
		this.priority = priority;
	}

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

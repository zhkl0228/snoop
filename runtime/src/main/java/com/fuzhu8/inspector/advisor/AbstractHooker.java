/**
 * 
 */
package com.fuzhu8.inspector.advisor;


/**
 * @author zhkl0228
 *
 */
public abstract class AbstractHooker<T> implements Hooker<T> {
	
	protected abstract void log(String msg);
	protected abstract void log(Throwable t);

	@Override
	public void log(Object msg) {
		if(msg instanceof Throwable) {
			log(Throwable.class.cast(msg));
		} else if(msg != null) {
			log(msg.toString());
		}
	}

}

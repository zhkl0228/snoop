/**
 * 
 */
package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public interface Command {
	
	void execute(StringBuffer lua, Inspector inspector);
	
	boolean isHelp();

}

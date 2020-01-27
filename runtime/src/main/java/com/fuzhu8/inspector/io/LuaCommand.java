/**
 * 
 */
package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public class LuaCommand implements Command {
	
	private final String lua;

	public LuaCommand(String lua) {
		super();
		this.lua = lua;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.Command#execute(java.lang.StringBuffer, com.fuzhu8.inspector.Inspector)
	 */
	@Override
	public void execute(StringBuffer lua, Inspector inspector) {
		inspector.evalLuaScript(this.lua);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.Command#isHelp()
	 */
	@Override
	public boolean isHelp() {
		return false;
	}

}

/**
 * 
 */
package com.fuzhu8.inspector.io;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public class TextCommand implements Command {
	
	private final String text;

	public TextCommand(String text) {
		super();
		this.text = text;
	}

	@Override
	public void execute(StringBuffer lua, Inspector inspector) {
		if("eval".equals(text)) {
			try {
				inspector.evalLuaScript(lua.toString());
			} finally {
				lua.setLength(0);
			}
			return;
		}
		
		lua.append(text).append('\n');
	}

	@Override
	public boolean isHelp() {
		return "help".equalsIgnoreCase(text) || "?".equals(text);
	}

}

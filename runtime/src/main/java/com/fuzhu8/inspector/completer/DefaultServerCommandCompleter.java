package com.fuzhu8.inspector.completer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.io.AbstractInspectCache;
import com.fuzhu8.inspector.io.InspectCache;

/**
 * @author zhkl0228
 *
 */
public class DefaultServerCommandCompleter extends AbstractInspectCache implements ServerCommandCompleter, InspectCache {
	
	private final Inspector inspector;
	private final String prefix;
	private final Map<String, String[]> map = new HashMap<>();

	public DefaultServerCommandCompleter(Inspector inspector, String prefix) {
		super(0x2002, true);
		this.inspector = inspector;
		this.prefix = prefix;
	}

	@Override
	public ServerCommandCompleter addCommandHelp(String command, String... help) {
		this.map.put(command, help);
		return this;
	}

	@Override
	protected void outputBody(DataOutputStream out) throws IOException {
		out.writeUTF(prefix == null ? "" : prefix);
		out.writeShort(map.size());
		for(Map.Entry<String, String[]> entry : map.entrySet()) {
			out.writeUTF(entry.getKey());
			out.writeByte(entry.getValue().length);
			for(String str : entry.getValue()) {
				out.writeUTF(str);
			}
		}
	}

	@Override
	public void commit() {
		this.inspector.writeToConsole(this);
	}

	@Override
	public String describeHelp() {
		StringBuilder buffer = new StringBuilder();
		for(String[] values : map.values()) {
			for(String str : values) {
				buffer.append(str).append('\n');
			}
		}
		return buffer.toString();
	}

}

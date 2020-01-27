/**
 * 
 */
package com.fuzhu8.inspector.completer;

/**
 * @author zhkl0228
 *
 */
public interface ServerCommandCompleter {
	
	ServerCommandCompleter addCommandHelp(String command, String...help);
	
	void commit();
	
	String describeHelp();

}

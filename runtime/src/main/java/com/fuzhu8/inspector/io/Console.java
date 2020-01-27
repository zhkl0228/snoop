package com.fuzhu8.inspector.io;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 * @author zhkl0228
 *
 */
public interface Console {
	
	void close();
	
	@Deprecated
	void write(byte[] data) throws IOException;
	
	OutputStream getOutputStream();
	
	Command readCommand() throws IOException;
	
	void open(Object obj) throws IOException;
	
	InetAddress getPeerAddress();

}

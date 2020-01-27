/**
 * 
 */
package com.fuzhu8.inspector.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

/**
 * @author zhkl0228
 *
 */
public class SocketConsole implements Console {
	
	private Socket socket;
	
	private DataInputStream reader;
	private OutputStream outputStream;
	
	public synchronized void open(Object obj) throws IOException {
		this.socket = (Socket) obj;
		this.outputStream = socket.getOutputStream();
		this.reader = new DataInputStream(socket.getInputStream());
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.Console#close()
	 */
	@Override
	public synchronized void close() {
		try { socket.close(); } catch(Exception e) {}
		IOUtils.closeQuietly(reader);
		IOUtils.closeQuietly(outputStream);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.Console#write(byte[])
	 */
	@Override
	public synchronized void write(byte[] data) throws IOException {
		outputStream.write(data);
		outputStream.flush();
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public InetAddress getPeerAddress() {
		InetSocketAddress socketAddress = (InetSocketAddress) (socket == null ? null : socket.getRemoteSocketAddress());
		return socketAddress == null ? null : socketAddress.getAddress();
	}

	@Override
	public Command readCommand() throws IOException {
		int type = reader.readUnsignedShort();
		switch (type) {
		case 0:
			return new TextCommand(reader.readUTF());
		case 1:
			int length = reader.readInt();
			byte[] data = new byte[length];
			reader.readFully(data);
			return new LuaCommand(new String(data, "UTF-8"));
		default:
			throw new IllegalArgumentException("Unknown command: " + type);
		}
	}

}

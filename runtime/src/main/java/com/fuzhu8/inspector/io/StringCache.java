/**
 * 
 */
package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author zhkl0228
 *
 */
public class StringCache extends SaveFileCache implements InspectCache {
	
	private final String data;

	public StringCache(String filename, String data) {
		super(filename);
		
		this.data = data;
	}

	@Override
	protected void writeFileData(DataOutputStream out) throws IOException {
		byte[] data = this.data.getBytes("UTF-8");
		out.writeInt(data.length);
		out.write(data, 0, data.length);
	}

}

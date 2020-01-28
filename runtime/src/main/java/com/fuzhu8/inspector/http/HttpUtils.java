package com.fuzhu8.inspector.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author zhkl0228
 *
 */
public class HttpUtils {
	
	private static final int CONNECT_TIMEOUT = 10000;
	private static final int READ_TIMEOUT = 15000;

	private static byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int read;
		byte[] buf = new byte[1024];
		while ((read = inputStream.read(buf)) != -1) {
			baos.write(buf, 0, read);
		}
		return baos.toByteArray();
	}

	public static byte[] sendGet(String urlStr) throws IOException {
		HttpURLConnection conn = null;
		InputStream inputStream = null;
		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(CONNECT_TIMEOUT);
			conn.setReadTimeout(READ_TIMEOUT);
			
			inputStream = conn.getInputStream();
			
			return toByteArray(inputStream);
		} finally {
			try { if(inputStream != null) inputStream.close(); } catch(Exception ignored) {}
			
			if(conn != null) {
				conn.disconnect();
			}
		}
	}

}

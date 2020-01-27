package com.fuzhu8.inspector.http;

import org.apache.commons.io.IOUtils;

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
			
			return IOUtils.toByteArray(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
			
			if(conn != null) {
				conn.disconnect();
			}
		}
	}

}

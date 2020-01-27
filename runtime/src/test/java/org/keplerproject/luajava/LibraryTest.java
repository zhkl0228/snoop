package org.keplerproject.luajava;

import com.sun.jna.Platform;

import junit.framework.TestCase;

/**
 * @author zhkl0228
 *
 */
public class LibraryTest extends TestCase {
	
	public void testLibraryName() {
		String mapName = System.mapLibraryName("luajava-1.1");
		if(Platform.isMac()) {
			assertEquals("libluajava-1.1.dylib", mapName);
		}
	}

}

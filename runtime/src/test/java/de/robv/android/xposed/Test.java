package de.robv.android.xposed;

public class Test {
	
	public Test() {
		super();
		
		System.out.println("Test Constructor");
	}

	static void test() {
		testBoolean();
		testByte();
		testInt();
		testChar();
		testShort();
		testLong();
		testFloat();
		testDouble();
		testString();
		testObject();
		testArray();
	}
	
	private static boolean testBoolean() {
		return Boolean.TRUE;
	}
	private static byte testByte() {
		return Byte.MAX_VALUE;
	}
	private static int testInt() {
		return Integer.MAX_VALUE;
	}
	private static char testChar() {
		return Character.MIN_SURROGATE;
	}
	private static short testShort() {
		return Short.MAX_VALUE;
	}
	private static long testLong() {
		return Long.MAX_VALUE;
	}
	private static float testFloat() {
		return Float.MAX_VALUE;
	}
	private static double testDouble() {
		return Double.MAX_VALUE;
	}
	private static String testString() {
		return String.valueOf(Boolean.TRUE);
	}
	private static Object testObject() {
		return System.out;
	}
	private static byte[] testArray() {
		return new byte[0];
	}

}

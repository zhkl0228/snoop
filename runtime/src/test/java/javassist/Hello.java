package javassist;

/**
 * @author zhkl0228
 *
 */
public class Hello {

    static {
        System.out.println("Hello initializer");
    }
	
	String say() {
        System.out.println("Hello");
        return "World";
    }

}

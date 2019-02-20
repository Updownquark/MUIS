package org.quick.test.util;

public class QuickTestUtils {
	public static String reverse(String string) {
		char[] c = string.toCharArray();
		for (int i = 0; i < c.length / 2; i++) {
			char temp = c[i];
			c[i] = c[c.length - i - 1];
			c[c.length - i - 1] = temp;
		}
		return new String(c);
	}
}

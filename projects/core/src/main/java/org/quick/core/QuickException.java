package org.quick.core;

/**
 * An exception thrown from various pieces of the web application markup architecture
 */
public class QuickException extends Exception {
	/**
	 * @see Exception#Exception(String)
	 */
	public QuickException(String message) {
		super(message);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public QuickException(String message, Throwable cause) {
		super(message, cause);
	}
}

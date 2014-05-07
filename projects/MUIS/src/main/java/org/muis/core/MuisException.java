package org.muis.core;

/**
 * An exception thrown from various pieces of the web application markup architecture
 */
public class MuisException extends Exception {
	/**
	 * @see Exception#Exception(String)
	 */
	public MuisException(String message) {
		super(message);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public MuisException(String message, Throwable cause) {
		super(message, cause);
	}
}

package org.quick.core.parser;

/** An exception that may result from an unrecoverable parsing error */
public class QuickParseException extends org.quick.core.QuickException
{
	/** @see org.quick.core.QuickException#QuickException(String) */
	public QuickParseException(String message) {
		super(message);
	}

	/** @see org.quick.core.QuickException#QuickException(String, Throwable) */
	public QuickParseException(String message, Throwable cause) {
		super(message, cause);
	}
}

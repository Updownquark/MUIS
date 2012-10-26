package org.muis.core.parser;

/**
 * An exception that may result from an unrecoverable parsing error
 */
public class MuisParseException extends org.muis.core.MuisException
{
	/**
	 * @see org.muis.core.MuisException#MuisException(String)
	 */
	public MuisParseException(String message)
	{
		super(message);
	}

	/**
	 * @see org.muis.core.MuisException#MuisException(String, Throwable)
	 */
	public MuisParseException(String message, Throwable cause)
	{
		super(message, cause);
	}
}

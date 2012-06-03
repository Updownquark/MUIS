package org.wam.parser;

/**
 * An exception that may result from an unrecoverable parsing error
 */
public class WamParseException extends org.wam.core.WamException
{
	/**
	 * @see org.wam.core.WamException#WamException(String)
	 */
	public WamParseException(String message)
	{
		super(message);
	}

	/**
	 * @see org.wam.core.WamException#WamException(String, Throwable)
	 */
	public WamParseException(String message, Throwable cause)
	{
		super(message, cause);
	}
}

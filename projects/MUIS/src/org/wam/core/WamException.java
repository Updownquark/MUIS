package org.wam.core;

/**
 * An exception thrown from various pieces of the web application markup architecture
 */
public class WamException extends Exception
{
	/**
	 * @see Exception#Exception(String)
	 */
	public WamException(String message)
	{
		super(message);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public WamException(String message, Throwable cause)
	{
		super(message, cause);
	}
}

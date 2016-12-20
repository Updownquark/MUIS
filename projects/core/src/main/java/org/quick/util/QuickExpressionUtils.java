package org.quick.util;

import org.quick.core.parser.QuickParseException;

/** Utility functions to be utilized directly from property values in Quick */
public class QuickExpressionUtils {
	/**
	 * @param <T> The type of the value
	 * @param value The value to check
	 * @return The value if it is non-null
	 * @throws QuickParseException If the value is null
	 */
	public static <T> T notNull(T value) throws QuickParseException {
		if (value == null)
			throw new QuickParseException("Value must not be null");
		return value;
	}

	/**
	 * @param <T> The type of the value
	 * @param value The value to check
	 * @param msg The message to append to the error if the value is null
	 * @return The value if it is non-null
	 * @throws QuickParseException If the value is null
	 */
	public static <T> T notNull(T value, String msg) throws QuickParseException {
		if (value == null)
			throw new QuickParseException("Value must not be null: " + msg);
		return value;
	}
}

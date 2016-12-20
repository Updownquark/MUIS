package org.quick.util;

import org.quick.core.parser.QuickParseException;

public class QuickExpressionUtils {
	public static <T> T notNull(T value) throws QuickParseException {
		if (value == null)
			throw new QuickParseException("Value must not be null");
		return value;
	}

	public static <T> T notNull(T value, String msg) throws QuickParseException {
		if (value == null)
			throw new QuickParseException("Value must not be null: " + msg);
		return value;
	}
}

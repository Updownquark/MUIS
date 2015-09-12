package org.quick.util;

/** Intended to carry a non-runtime exception up the stack where it will be caught and processed properly */
public class ExceptionWrapper extends RuntimeException {
	/** @see RuntimeException#RuntimeException(Throwable) */
	public ExceptionWrapper(Throwable cause) {
		super(cause);
	}
}

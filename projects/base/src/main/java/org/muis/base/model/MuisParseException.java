package org.muis.base.model;

public class MuisParseException extends Exception {
	private final int theErrorStart;

	private final int theErrorEnd;

	public MuisParseException(String message, int errorStart, int errorEnd) {
		super(message);
		theErrorStart = errorStart;
		theErrorEnd = errorEnd;
	}

	public MuisParseException(Throwable cause, int errorStart, int errorEnd) {
		super(cause);
		theErrorStart = errorStart;
		theErrorEnd = errorEnd;
	}

	public MuisParseException(String message, Throwable cause, int errorStart, int errorEnd) {
		super(message, cause);
		theErrorStart = errorStart;
		theErrorEnd = errorEnd;
	}

	public int getErrorStart() {
		return theErrorStart;
	}

	public int getErrorEnd() {
		return theErrorEnd;
	}
}

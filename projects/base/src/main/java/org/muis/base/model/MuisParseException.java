package org.muis.base.model;

/** Thrown from a {@link MuisFormatter}'s {@link MuisFormatter#parse(org.muis.core.model.MuisDocumentModel) parse} method when parsing fails */
public class MuisParseException extends Exception {
	private final int theErrorStart;

	private final int theErrorEnd;

	/**
	 * @param message The message informing the user why the input could not be parsed
	 * @param errorStart The start position (inclusive) of the interval in the input where the error occurred
	 * @param errorEnd The end position (exclusive) of the interval in the input where the error occurred
	 */
	public MuisParseException(String message, int errorStart, int errorEnd) {
		super(message);
		theErrorStart = errorStart;
		theErrorEnd = errorEnd;
	}

	/**
	 * @param cause The exception that may have precipitated this exception
	 * @param errorStart The start position (inclusive) of the interval in the input where the error occurred
	 * @param errorEnd The end position (exclusive) of the interval in the input where the error occurred
	 */
	public MuisParseException(Throwable cause, int errorStart, int errorEnd) {
		super(cause);
		theErrorStart = errorStart;
		theErrorEnd = errorEnd;
	}

	/**
	 * @param message The message informing the user why the input could not be parsed
	 * @param cause The exception that may have precipitated this exception
	 * @param errorStart The start position (inclusive) of the interval in the input where the error occurred
	 * @param errorEnd The end position (exclusive) of the interval in the input where the error occurred
	 */
	public MuisParseException(String message, Throwable cause, int errorStart, int errorEnd) {
		super(message, cause);
		theErrorStart = errorStart;
		theErrorEnd = errorEnd;
	}

	/** @return The start position of the interval in the parsed text where the error occurs */
	public int getErrorStart() {
		return theErrorStart;
	}

	/** @return The end position (exclusive) of the interval in the parsed text where the error occurs */
	public int getErrorEnd() {
		return theErrorEnd;
	}
}

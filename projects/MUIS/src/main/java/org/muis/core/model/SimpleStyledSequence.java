package org.muis.core.model;

import org.muis.core.model.MuisDocumentModel.StyledSequence;
import org.muis.core.style.MuisStyle;

/** A simple styled sequence */
public class SimpleStyledSequence implements StyledSequence {
	private final String theValue;

	private final MuisStyle theStyle;

	/**
	 * @param value The content for the sequence
	 * @param style The style for the sequence
	 */
	public SimpleStyledSequence(String value, MuisStyle style) {
		theValue = value;
		theStyle = style;
	}

	@Override
	public int length() {
		return theValue.length();
	}

	@Override
	public char charAt(int index) {
		return theValue.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return theValue.subSequence(start, end);
	}

	@Override
	public MuisStyle getStyle() {
		return theStyle;
	}
}

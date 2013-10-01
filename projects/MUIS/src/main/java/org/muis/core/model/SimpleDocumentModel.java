package org.muis.core.model;

import java.util.Iterator;

import org.muis.core.style.MuisStyle;

/** A very simple document model that uses a single style and keeps a single, mutable set of content */
public class SimpleDocumentModel extends AbstractMuisDocumentModel {
	private final MuisStyle theStyle;

	private final StringBuffer theContent;

	/** @param style The style for this document */
	public SimpleDocumentModel(MuisStyle style) {
		theStyle = style;
		theContent = new StringBuffer();
	}

	/** @return This document's style */
	public MuisStyle getStyle() {
		return theStyle;
	}

	/** @return This document's content */
	public StringBuffer getContent() {
		return theContent;
	}

	@Override
	public Iterator<StyledSequence> iterator() {
		final String content = theContent.toString();
		return prisms.util.ArrayUtils.iterator(new StyledSequence[] {new StyledSequence() {
			@Override
			public int length() {
				return content.length();
			}

			@Override
			public char charAt(int index) {
				return content.charAt(index);
			}

			@Override
			public CharSequence subSequence(int start, int end) {
				return content.subSequence(end, end);
			}

			@Override
			public MuisStyle getStyle() {
				return theStyle;
			}
		}}, true);
	}
}

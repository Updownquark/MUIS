package org.muis.core.parser;

/** Represents textual data from a MUIS file */
public class MuisText extends MuisContent {
	private final String theContent;

	private final boolean isCData;

	/**
	 * @param parent The parent structure of this text
	 * @param content The text content
	 * @param cdata Whether this content represents a CDATA block
	 */
	public MuisText(WidgetStructure parent, String content, boolean cdata) {
		super(parent);
		theContent = content;
		isCData = cdata;
	}

	/** @return The text content */
	public String getContent() {
		return theContent;
	}

	/** @return Whether this content represents a CDATA block */
	public boolean isCData() {
		return isCData;
	}
}

package org.quick.core.parser;

/** Represents textual data from a Quick file */
public class QuickText extends QuickContent {
	private final String theContent;

	private final boolean isCData;

	/**
	 * @param parent The parent structure of this text
	 * @param content The text content
	 * @param cdata Whether this content represents a CDATA block
	 */
	public QuickText(WidgetStructure parent, String content, boolean cdata) {
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

package org.muis.core.parser;

public class MuisText extends MuisContent {
	private final String theContent;

	private final boolean isCData;

	public MuisText(WidgetStructure parent, String content, boolean cdata) {
		super(parent);
		theContent = content;
		isCData = cdata;
	}

	public String getContent() {
		return theContent;
	}

	public boolean isCData() {
		return isCData;
	}
}

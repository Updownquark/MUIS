package org.muis.core.parser;


/** Represents MUIS content as defined in XML */
public abstract class MuisContent {
	private final WidgetStructure theParent;

	public MuisContent(WidgetStructure parent) {
		theParent = parent;
	}

	public WidgetStructure getParent() {
		return theParent;
	}
}

package org.muis.core.parser;

/** Represents MUIS content as defined in XML */
public abstract class MuisContent {
	private final WidgetStructure theParent;

	/** @param parent The parent structure of this content */
	public MuisContent(WidgetStructure parent) {
		theParent = parent;
	}

	/** @return The parent structure of this content */
	public WidgetStructure getParent() {
		return theParent;
	}
}

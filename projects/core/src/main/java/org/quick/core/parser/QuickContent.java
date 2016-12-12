package org.quick.core.parser;

/** Represents Quick content as defined in XML */
public abstract class QuickContent {
	private final WidgetStructure theParent;

	/** @param parent The parent structure of this content */
	public QuickContent(WidgetStructure parent) {
		theParent = parent;
	}

	/** @return The parent structure of this content */
	public WidgetStructure getParent() {
		return theParent;
	}
}

package org.quick.core.parser;

import org.quick.core.QuickHeadSection;

/** Represents the parsed structure of a MUIS document */
public class QuickDocumentStructure {
	private final java.net.URL theLocation;

	private final QuickHeadSection theHead;

	private final WidgetStructure theContent;

	/**
	 * @param location The location of the XML file parsed into this structure
	 * @param head The head section containing metadata for the MUIS document
	 * @param content The root content structure of the document
	 */
	public QuickDocumentStructure(java.net.URL location, QuickHeadSection head, WidgetStructure content) {
		theLocation = location;
		theHead = head;
		theContent = content;
	}

	/** @return The location of the XML file parsed into this structure */
	public java.net.URL getLocation() {
		return theLocation;
	}

	/** @return The had section containing metadata for the MUIS document */
	public QuickHeadSection getHead() {
		return theHead;
	}

	/** @return The root content structure of the document */
	public WidgetStructure getContent() {
		return theContent;
	}
}

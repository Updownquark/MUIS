package org.quick.core.parser;

/** Represents the parsed structure of a Quick document */
public class QuickDocumentStructure {
	private final java.net.URL theLocation;

	private final QuickHeadStructure theHead;

	private final WidgetStructure theContent;

	/**
	 * @param location The location of the XML file parsed into this structure
	 * @param head The head section containing metadata for the Quick document
	 * @param content The root content structure of the document
	 */
	public QuickDocumentStructure(java.net.URL location, QuickHeadStructure head, WidgetStructure content) {
		theLocation = location;
		theHead = head;
		theContent = content;
	}

	/** @return The location of the XML file parsed into this structure */
	public java.net.URL getLocation() {
		return theLocation;
	}

	/** @return The had section containing metadata for the Quick document */
	public QuickHeadStructure getHead() {
		return theHead;
	}

	/** @return The root content structure of the document */
	public WidgetStructure getContent() {
		return theContent;
	}
}

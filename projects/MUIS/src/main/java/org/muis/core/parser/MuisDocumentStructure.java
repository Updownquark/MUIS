package org.muis.core.parser;

import org.muis.core.MuisDocument;

/** Represents the parsed structure of a MUIS document */
public class MuisDocumentStructure {
	private final MuisDocument theDocument;

	private final WidgetStructure theContent;

	/**
	 * @param doc The MUIS document that this structure is for
	 * @param content The root content structure of the document
	 */
	public MuisDocumentStructure(MuisDocument doc, WidgetStructure content) {
		theDocument = doc;
		theContent = content;
	}

	/** @return The MUIS document that this structure is for */
	public MuisDocument getDocument() {
		return theDocument;
	}

	/** @return The root content structure of the document */
	public WidgetStructure getContent() {
		return theContent;
	}
}

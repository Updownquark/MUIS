package org.muis.core.parser;

import org.muis.core.MuisDocument;

public class MuisDocumentStructure {
	private final MuisDocument theDocument;

	private final WidgetStructure theContent;

	public MuisDocumentStructure(MuisDocument doc, WidgetStructure content) {
		theDocument = doc;
		theContent = content;
	}

	public MuisDocument getDocument() {
		return theDocument;
	}

	public WidgetStructure getContent() {
		return theContent;
	}
}

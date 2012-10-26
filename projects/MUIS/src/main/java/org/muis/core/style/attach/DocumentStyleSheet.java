package org.muis.core.style.attach;

import org.muis.core.style.sheet.AbstractStyleSheet;
import org.muis.core.style.sheet.StyleSheet;

/**
 * The {@link StyleSheet} at {@link org.muis.core.MuisDocument document}-level that incorporates the attributes of all style sheets set in
 * the document
 */
public class DocumentStyleSheet extends AbstractStyleSheet {
	private final org.muis.core.MuisDocument theDoc;

	/** @param doc The document that the style sheet is for */
	public DocumentStyleSheet(org.muis.core.MuisDocument doc) {
		theDoc = doc;
		addDependency(theDoc.getEnvironment().getStyle(), null);
	}

	/** @return The document that this style is for */
	public org.muis.core.MuisDocument getDocument() {
		return theDoc;
	}

	/** @param styleSheet The style sheet to add to the document */
	public void addStyleSheet(StyleSheet styleSheet) {
		addDependency(styleSheet, null);
	}

	/** @param styleSheet The style sheet to add to the document */
	public void removeStyleSheet(StyleSheet styleSheet) {
		removeDependency(styleSheet);
	}
}
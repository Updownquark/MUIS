package org.muis.core.style.attach;

import org.muis.core.style.sheet.AbstractStyleSheet;
import org.muis.core.style.sheet.StyleSheet;

/**
 * The {@link StyleSheet} at {@link org.muis.core.MuisDocument document}-level that incorporates the attributes of all style sheets set in
 * the document
 */
public class DocumentStyleSheet extends AbstractStyleSheet {
	/** @param styleSheet The style sheet to add to the document */
	public void addStyleSheet(StyleSheet styleSheet) {
		addDependency(styleSheet);
	}

	/** @param styleSheet The style sheet to add to the document */
	public void removeStyleSheet(StyleSheet styleSheet) {
		removeDependency(styleSheet);
	}
}

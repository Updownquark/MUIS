package org.muis.core.style.attach;

import org.muis.core.style.sheet.AbstractStyleSheet;
import org.muis.core.style.sheet.StyleSheet;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;

/**
 * The {@link StyleSheet} at {@link org.muis.core.MuisDocument document}-level that incorporates the attributes of all style sheets set in
 * the document
 */
public class DocumentStyleSheet extends AbstractStyleSheet {
	private final org.muis.core.MuisDocument theDoc;
	private final java.util.List<StyleSheet> theDependencyController;

	/** @param doc The document that the style sheet is for */
	public DocumentStyleSheet(org.muis.core.MuisDocument doc) {
		super(ObservableUtils.control(new ObservableArrayList<>(new prisms.lang.Type(StyleSheet.class))));
		theDependencyController = ObservableUtils.getController(getConditionalDependencies());
		theDoc = doc;
		theDependencyController.add(0, theDoc.getEnvironment().getStyle());
	}

	/** @return The document that this style is for */
	public org.muis.core.MuisDocument getDocument() {
		return theDoc;
	}

	/** @param styleSheet The style sheet to add to the document */
	public void addStyleSheet(StyleSheet styleSheet) {
		theDependencyController.add(styleSheet);
	}

	/** @param styleSheet The style sheet to add to the document */
	public void removeStyleSheet(StyleSheet styleSheet) {
		theDependencyController.remove(styleSheet);
	}

	@Override
	public String toString() {
		String path = theDoc.getLocation().getPath();
		int lastSlash = path.lastIndexOf('/');
		if(lastSlash >= 0)
			path = path.substring(lastSlash + 1);
		return "Style sheet for " + path;
	}
}

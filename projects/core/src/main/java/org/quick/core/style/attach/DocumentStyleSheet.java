package org.quick.core.style.attach;

import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;
import org.quick.core.style.sheet.AbstractStyleSheet;
import org.quick.core.style.sheet.StyleSheet;

import com.google.common.reflect.TypeToken;

/**
 * The {@link StyleSheet} at {@link org.quick.core.QuickDocument document}-level that incorporates the attributes of all style sheets set in
 * the document
 */
public class DocumentStyleSheet extends AbstractStyleSheet {
	private final org.quick.core.QuickDocument theDoc;
	private final java.util.List<StyleSheet> theDependencyController;

	/** @param doc The document that the style sheet is for */
	public DocumentStyleSheet(org.quick.core.QuickDocument doc) {
		super(doc.msg(), ObservableUtils.control(new ObservableArrayList<>(TypeToken.of(StyleSheet.class))));
		theDependencyController = ObservableUtils.getController(getConditionalDependencies());
		theDoc = doc;
		theDependencyController.add(0, theDoc.getEnvironment().getStyle());
	}

	/** @return The document that this style is for */
	public org.quick.core.QuickDocument getDocument() {
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

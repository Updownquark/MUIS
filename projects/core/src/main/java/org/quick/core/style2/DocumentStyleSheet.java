package org.quick.core.style2;

import org.observe.collect.ObservableList;
import org.quick.core.QuickDocument;

import com.google.common.reflect.TypeToken;

/** A {@link QuickDocument}'s style */
public class DocumentStyleSheet extends CompoundStyleSheet {
	private final QuickDocument theDocument;
	private final ObservableList<StyleSheet> theExternalStyleSheets;

	private DocumentStyleSheet(QuickDocument doc, ObservableList<StyleSheet> externalStyleSheets) {
		super(ObservableList.flattenLists(TypeToken.of(StyleSheet.class), //
			ObservableList.constant(TypeToken.of(StyleSheet.class), doc.getEnvironment().getStyle()), //
			ObservableList.constant(TypeToken.of(StyleSheet.class), doc.getHead().getStyleSheets()), //
			externalStyleSheets));
		theDocument=doc;
		theExternalStyleSheets = externalStyleSheets;
	}

	/** @return The document that this style sheet belongs to */
	public QuickDocument getDocument() {
		return theDocument;
	}

	/** @return A mutable list of the extra style sheets pulled in directly from the document */
	public ObservableList<StyleSheet> getExternalStyleSheets() {
		return theExternalStyleSheets;
	}

	/**
	 * @param doc The document to build the style sheet for
	 * @return The new document style sheet
	 */
	public static DocumentStyleSheet build(QuickDocument doc) {
		return new DocumentStyleSheet(doc, ObservableList.constant(TypeToken.of(StyleSheet.class)));
	}
}

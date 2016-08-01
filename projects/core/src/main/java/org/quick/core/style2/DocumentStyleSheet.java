package org.quick.core.style2;

import org.observe.collect.ObservableList;
import org.quick.core.QuickDocument;

import com.google.common.reflect.TypeToken;

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

	public ObservableList<StyleSheet> getExternalStyleSheets() {
		return theExternalStyleSheets;
	}

	public static DocumentStyleSheet build(QuickDocument doc) {
		return new DocumentStyleSheet(doc, ObservableList.constant(TypeToken.of(StyleSheet.class)));
	}
}

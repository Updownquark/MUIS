package org.quick.core.style;

import org.observe.Observable;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.quick.core.QuickDocument;

import com.google.common.reflect.TypeToken;

/** A {@link QuickDocument}'s style */
public class DocumentStyleSheet extends CompoundStyleSheet {
	private static final TypeToken<ObservableCollection<StyleSheet>> SS_COLL_TYPE = TypeTokens.get().keyFor(ObservableCollection.class)
		.getCompoundType(StyleSheet.class, ss -> new TypeToken<ObservableCollection<StyleSheet>>() {});
	private final QuickDocument theDocument;
	private final ObservableCollection<StyleSheet> theExternalStyleSheets;

	private DocumentStyleSheet(QuickDocument doc, ObservableCollection<StyleSheet> externalStyleSheets) {
		super(flatten(doc.getDispose(), externalStyleSheets, //
			ObservableCollection.of(TypeTokens.get().of(StyleSheet.class), doc.getHead().getStyleSheets()), //
			ObservableCollection.of(TypeTokens.get().of(StyleSheet.class), doc.getEnvironment().getStyle())), doc.getDispose());
		theDocument = doc;
		theExternalStyleSheets = externalStyleSheets;
	}

	private static ObservableCollection<StyleSheet> flatten(Observable<?> death, ObservableCollection<StyleSheet>... styleSheetSets) {
		return ObservableCollection.of(SS_COLL_TYPE, styleSheetSets).flow().flatMap(TypeTokens.get().of(StyleSheet.class), //
			sss -> sss.flow()).collectActive(death);
	}

	/** @return The document that this style sheet belongs to */
	public QuickDocument getDocument() {
		return theDocument;
	}

	/** @return A mutable list of the extra style sheets pulled in directly from the document */
	public ObservableCollection<StyleSheet> getExternalStyleSheets() {
		return theExternalStyleSheets;
	}

	/**
	 * @param doc The document to build the style sheet for
	 * @return The new document style sheet
	 */
	public static DocumentStyleSheet build(QuickDocument doc) {
		return new DocumentStyleSheet(doc, ObservableCollection.of(TypeTokens.get().of(StyleSheet.class)));
	}
}

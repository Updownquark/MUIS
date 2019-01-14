package org.quick.core.style;

import java.util.concurrent.ConcurrentHashMap;

import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.collect.impl.CachingTreeSet;
import org.observe.util.TypeTokens;
import org.quick.core.QuickDocument;

import com.google.common.reflect.TypeToken;

/** A {@link QuickDocument}'s style */
public class DocumentStyleSheet extends CompoundStyleSheet {
	private static final TypeToken STYLE_ATTR_TYPE = new TypeToken<StyleAttribute<?>>() {};
	private final QuickDocument theDocument;
	private final ObservableCollection<StyleSheet> theExternalStyleSheets;
	private final ObservableSet<StyleAttribute<?>> theCachedAttributes;
	private final ConcurrentHashMap<StyleAttribute<?>, ObservableSortedSet<? extends StyleConditionValue<?>>> theCachedValues;

	private DocumentStyleSheet(QuickDocument doc, ObservableCollection<StyleSheet> externalStyleSheets) {
		super(ObservableCollection.flattenLists(TypeToken.of(StyleSheet.class), //
			externalStyleSheets, //
			ObservableCollection.of(TypeTokens.get().of(StyleSheet.class), doc.getHead().getStyleSheets()), //
			ObservableCollection.of(TypeTokens.get().of(StyleSheet.class), doc.getEnvironment().getStyle())));
		theDocument = doc;
		theExternalStyleSheets = externalStyleSheets;
		// Cache these for performance
		theCachedAttributes = ObservableSet.create(STYLE_ATTR_TYPE);
		doc.getDispose().take(1).act(v -> theCachedAttributes.unsubscribe());
		theCachedValues = new ConcurrentHashMap<>();
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

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theCachedAttributes;
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		return (ObservableSortedSet<StyleConditionValue<T>>) theCachedValues.computeIfAbsent(attr,
			att -> {
				CachingTreeSet<StyleConditionValue<T>> treeSet = new CachingTreeSet<>(super.getStyleExpressions(attr));
				theDocument.getDispose().take(1).act(v -> treeSet.unsubscribe());
				return treeSet;
			});
	}
}

package org.quick.core.style;

import java.util.concurrent.ConcurrentHashMap;

import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.collect.impl.CachingHashSet;
import org.observe.collect.impl.CachingTreeSet;
import org.quick.core.QuickDocument;

import com.google.common.reflect.TypeToken;

/** A {@link QuickDocument}'s style */
public class DocumentStyleSheet extends CompoundStyleSheet {
	private final QuickDocument theDocument;
	private final ObservableList<StyleSheet> theExternalStyleSheets;
	private final CachingHashSet<StyleAttribute<?>> theCachedAttributes;
	private final ConcurrentHashMap<StyleAttribute<?>, CachingTreeSet<? extends StyleConditionValue<?>>> theCachedValues;

	private DocumentStyleSheet(QuickDocument doc, ObservableList<StyleSheet> externalStyleSheets) {
		super(ObservableList.flattenLists(TypeToken.of(StyleSheet.class), //
			ObservableList.constant(TypeToken.of(StyleSheet.class), doc.getEnvironment().getStyle()), //
			ObservableList.constant(TypeToken.of(StyleSheet.class), doc.getHead().getStyleSheets()), //
			externalStyleSheets));
		theDocument=doc;
		theExternalStyleSheets = externalStyleSheets;
		// Cache these for performance
		theCachedAttributes = new CachingHashSet<>(super.attributes());
		theCachedValues = new ConcurrentHashMap<>();
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

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theCachedAttributes;
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		return (ObservableSortedSet<StyleConditionValue<T>>) theCachedValues.computeIfAbsent(attr,
			att -> new CachingTreeSet<>(super.getStyleExpressions(attr)));
	}
}

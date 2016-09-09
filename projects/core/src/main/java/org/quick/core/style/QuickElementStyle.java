package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;

/** The style on a {@link QuickElement} */
public class QuickElementStyle implements QuickStyle {
	private final QuickElement theElement;

	/** @param element The element that this style is for */
	public QuickElementStyle(QuickElement element) {
		theElement = element;
	}

	/** @return The element that this style is for */
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		ObservableValue<QuickStyle> localStyle = theElement.atts().getHolder(StyleAttributes.style);
		ObservableSet<StyleAttribute<?>> localAttrs = ObservableSet.flattenValue(localStyle.mapV(s -> s.attributes()));
		StyleSheet sheet = theElement.getDocument() == null ? null : theElement.getDocument().getStyle();
		if (sheet == null)
			return localAttrs;
		return ObservableSet.unique(ObservableCollection.flattenCollections(localAttrs, sheet.attributes()), Object::equals);
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		QuickStyle localStyle = theElement.atts().get(StyleAttributes.style);
		if (localStyle != null && localStyle.isSet(attr))
			return true;
		StyleSheet sheet = theElement.getDocument().getStyle();
		if (sheet.isSet(theElement, attr))
			return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<QuickStyle> localStyle = theElement.atts().getHolder(StyleAttributes.style);
		ObservableValue<T> localValue = ObservableValue.flatten(localStyle.mapV(s -> s.get(attr, false)));
		if (theElement.getDocument() == null)
			return localValue;
		StyleSheet sheet = theElement.getDocument().getStyle();
		return ObservableValue.firstValue(attr.getType().getType(), null, null, localValue, sheet.get(theElement, attr, withDefault));
	}
}

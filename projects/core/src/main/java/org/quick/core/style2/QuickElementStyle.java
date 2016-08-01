package org.quick.core.style2;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleAttributes;

public class QuickElementStyle implements QuickStyle {
	private final QuickElement theElement;

	public QuickElementStyle(QuickElement element) {
		theElement = element;
	}

	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		ObservableValue<QuickStyle> localStyle = theElement.atts().getHolder(StyleAttributes.STYLE_ATTRIBUTE);
		ObservableSet<StyleAttribute<?>> localAttrs = ObservableSet.flattenValue(localStyle.mapV(s -> s.attributes()));
		StyleSheet sheet = theElement.getDocument().getStyle();
		return ObservableSet.unique(ObservableCollection.flattenCollections(localAttrs, sheet.attributes()), Object::equals);
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		QuickStyle localStyle = theElement.atts().get(StyleAttributes.STYLE_ATTRIBUTE);
		if (localStyle != null && localStyle.isSet(attr))
			return true;
		StyleSheet sheet = theElement.getDocument().getStyle();
		if (sheet.isSet(theElement, attr))
			return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<QuickStyle> localStyle = theElement.atts().getHolder(StyleAttributes.STYLE_ATTRIBUTE);
		ObservableValue<T> localValue = ObservableValue.flatten(localStyle.mapV(s -> s.get(attr, false)));
		StyleSheet sheet = theElement.getDocument().getStyle();
		return ObservableValue.first(localValue, sheet.get(theElement, attr, withDefault));
	}
}

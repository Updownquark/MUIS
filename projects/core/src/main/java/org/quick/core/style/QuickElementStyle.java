package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

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
		ObservableSet<StyleAttribute<?>> parentAtts = ObservableSet
			.flattenValue(theElement.getParent().mapV(p -> p.getStyle().attributes())).filterStatic(att -> att.isInherited());
		ObservableCollection<StyleAttribute<?>> flattened;
		if (sheet == null)
			flattened = ObservableCollection.flattenCollections(localAttrs, parentAtts);
		else
			flattened = ObservableCollection.flattenCollections(localAttrs, parentAtts, sheet.attributes());
		return ObservableSet.unique(flattened, Object::equals);
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		QuickStyle localStyle = theElement.atts().get(StyleAttributes.style);
		if (localStyle != null && localStyle.isSet(attr))
			return true;
		StyleSheet sheet = theElement.getDocument().getStyle();
		if (sheet.isSet(theElement, attr))
			return true;
		if (attr.isInherited()) {
			QuickElement parent = theElement.getParent().get();
			if (parent != null && parent.getStyle().isSet(attr))
				return true;
		}
		return false;
	}

	@Override
	public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<QuickStyle> localStyle = theElement.atts().getHolder(StyleAttributes.style);
		ObservableValue<T> localValue = ObservableValue.flatten(localStyle.mapV(s -> s.get(attr, false)));
		if (attr.isInherited()) {
			ObservableValue<T> parentLocalValue = ObservableValue.flatten(theElement.getParent().mapV(p -> {
				if (p == null)
					return null;
				ObservableValue<QuickStyle> parentStyle = p.atts().getHolder(StyleAttributes.style);
				return ObservableValue.flatten(parentStyle.mapV(s -> s.get(attr, false)));
			}));
			localValue = ObservableValue.firstValue(attr.getType().getType(), null, null, localValue, parentLocalValue);
		}
		if (theElement.getDocument() == null)
			return localValue;
		StyleSheet sheet = theElement.getDocument().getStyle();
		ObservableValue<StyleConditionValue<T>> ssMatch = sheet.getBestMatch(theElement, attr);
		ObservableValue<T> ssValue;
		if (attr.isInherited()) {
			ObservableValue<StyleConditionValue<T>> parentSSMatch = ObservableValue.flatten(theElement.getParent().mapV(p -> {
				if (p == null)
					return null;
				return sheet.getBestMatch(p, attr);
			}));
			ssMatch = ObservableValue.firstValue(
				new TypeToken<StyleConditionValue<T>>() {}.where(new TypeParameter<T>() {}, attr.getType().getType()), null, null, ssMatch,
				parentSSMatch);
		}
		if (withDefault)
			ssValue = ObservableValue.flatten(ssMatch, () -> attr.getDefault());
		else
			ssValue = ObservableValue.flatten(ssMatch);
		return ObservableValue.firstValue(attr.getType().getType(), null, null, localValue, ssValue);
	}
}

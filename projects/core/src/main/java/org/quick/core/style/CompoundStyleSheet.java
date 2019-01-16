package org.quick.core.style;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.observe.Observable;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.util.TypeTokens;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/** A StyleSheet whose style values come from other StyleSheets */
public class CompoundStyleSheet implements StyleSheet {
	private final ObservableCollection<StyleSheet> theComponents;
	private final Observable<?> theDeath;
	private final ObservableSet<StyleAttribute<?>> theAttributes;
	private final Map<StyleAttribute<?>, ObservableSortedSet<? extends StyleConditionValue<?>>> theAttributeExpressions;

	/** @param components The style sheets that this style sheet will use as sources */
	public CompoundStyleSheet(ObservableCollection<StyleSheet> components, Observable<?> death) {
		theComponents = components;
		theDeath = death;
		theAttributes = theComponents.flow()
			.flatMap(//
				TypeTokens.get().keyFor(StyleAttribute.class).parameterized(() -> new TypeToken<StyleAttribute<?>>() {}),
				component -> component.attributes().flow())
			.distinct().collectActive(death);
		theAttributeExpressions = new ConcurrentHashMap<>();
	}

	/** @return The style sheets that this style sheet uses for sources */
	public ObservableCollection<StyleSheet> getComponents() {
		return theComponents;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theAttributes;
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		return (ObservableSortedSet<StyleConditionValue<T>>) theAttributeExpressions.computeIfAbsent(attr, //
			att -> {
				TypeToken<StyleConditionValue<T>> type = TypeTokens.get().keyFor(StyleConditionValue.class).getCompoundType(
					attr.getType().getType(), //
					t -> new TypeToken<StyleConditionValue<T>>() {}.where(new TypeParameter<T>() {}, t));
				return theComponents.flow().flatMap(type, component -> component.getStyleExpressions(attr).flow())//
					.distinctSorted(StyleConditionValue::compareTo, true).collectActive(theDeath);
			});
	}

	@Override
	public boolean isSet(StyleConditionInstance<?> condition, StyleAttribute<?> attr) {
		for (StyleSheet ss : theComponents)
			if (ss.isSet(condition, attr))
				return true;
		return false;
	}
}

package org.quick.core.style2;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;

public interface StyleSheet {
	ObservableSet<StyleAttribute<?>> attributes();

	<T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr);

	default boolean isSet(QuickElement element, StyleAttribute<?> attr) {
		for (StyleConditionValue<?> scv : getStyleExpressions(attr))
			if (scv.getCondition().matches(element).get())
				return true;
		return false;
	}

	default <T> T get(QuickElement element, StyleAttribute<T> attr, boolean withDefault) {
		for (StyleConditionValue<T> scv : getStyleExpressions(attr))
			if (scv.getCondition().matches(element).get())
				return scv.get();
		if (withDefault)
			return attr.getDefault();
		else
			return null;
	}

	default <T> ObservableValue<T> observe(QuickElement element, StyleAttribute<T> attr, boolean withDefault) {
	}
}

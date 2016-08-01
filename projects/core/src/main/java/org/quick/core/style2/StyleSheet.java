package org.quick.core.style2;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickState;
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

	default boolean isSet(QuickElement element, ObservableSet<QuickState> extraStates, StyleAttribute<?> attr) {
		for (StyleConditionValue<?> scv : getStyleExpressions(attr))
			if (scv.getCondition().matches(element, extraStates).get())
				return true;
		return false;
	}

	default <T> ObservableValue<T> get(QuickElement element, StyleAttribute<T> attr, boolean withDefault) {
		ObservableSortedSet<StyleConditionValue<T>> exprs = getStyleExpressions(attr)
			.filterStatic(scv -> scv.getCondition().getType().isInstance(element));
		exprs = exprs.refreshEach(scv -> scv.getCondition().matches(element)).filter(scv -> scv.getCondition().matches(element).get());
		ObservableValue<T> value = ObservableValue.flatten(exprs.getFirst());
		if (withDefault)
			value = value.mapV(v -> v == null ? attr.getDefault() : v);
		return value;
	}

	default <T> ObservableValue<T> get(QuickElement element, ObservableSet<QuickState> extraStates, StyleAttribute<T> attr,
		boolean withDefault) {
		ObservableSortedSet<StyleConditionValue<T>> exprs = getStyleExpressions(attr)
			.filterStatic(scv -> scv.getCondition().getType().isInstance(element));
		exprs = exprs.refreshEach(scv -> scv.getCondition().matches(element, extraStates))
			.filter(scv -> scv.getCondition().matches(element, extraStates).get());
		ObservableValue<T> value = ObservableValue.flatten(exprs.getFirst());
		if (withDefault)
			value = value.mapV(v -> v == null ? attr.getDefault() : v);
		return value;
	}
}

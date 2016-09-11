package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickState;

/** Contains property values that are conditional on various properties of an element */
public interface StyleSheet {
	/** @return All attributes for which conditional values are defined in this style sheet */
	ObservableSet<StyleAttribute<?>> attributes();

	/**
	 * @param <T> The type of the attribute
	 * @param The type of the attribute
	 * @param attr The style attribute to get all the conditional values for
	 * @return All conditional values defined in this style sheet for the given attribute
	 */
	<T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr);

	/**
	 * @param element The element to use for checking conditions
	 * @param attr The attribute to check
	 * @return Whether this style sheet has any values for the given attribute for which the condition is met for the given element
	 */
	default boolean isSet(QuickElement element, StyleAttribute<?> attr) {
		for (StyleConditionValue<?> scv : getStyleExpressions(attr))
			if (scv.getCondition().matches(element).get())
				return true;
		return false;
	}

	/**
	 * @param element The element to use for checking conditions
	 * @param extraStates The extra states to consider for the condition
	 * @param attr The style attribute to check
	 * @return Whether this style sheet has any values for the given attribute for which the condition is met for the given element with the
	 *         given extra states
	 */
	default boolean isSet(QuickElement element, ObservableSet<QuickState> extraStates, StyleAttribute<?> attr) {
		for (StyleConditionValue<?> scv : getStyleExpressions(attr))
			if (scv.getCondition().matches(element, extraStates).get())
				return true;
		return false;
	}

	/**
	 * @param <T> The type of the attribute
	 * @param element The element to use for checking conditions
	 * @param attr The style attribute to get the value for
	 * @param withDefault Whether to use the attribute's default value if this style sheet does not have a value whose condition matches the
	 *        element instead of null
	 * @return The value of the given attribute in this style sheet whose condition matches the given element
	 */
	default <T> ObservableValue<T> get(QuickElement element, StyleAttribute<T> attr, boolean withDefault) {
		ObservableSortedSet<StyleConditionValue<T>> exprs = getStyleExpressions(attr)
			.filterStatic(scv -> scv.getCondition().getType().isInstance(element));
		exprs = exprs.refreshEach(scv -> scv.getCondition().matches(element)).filter(scv -> scv.getCondition().matches(element).get());
		ObservableValue<T> value;
		if (withDefault)
			value = ObservableValue.flatten(exprs.getFirst(), () -> attr.getDefault());
		else
			value = ObservableValue.flatten(exprs.getFirst());
		return value;
	}

	/**
	 * @param <T> The type of the attribute
	 * @param element The element to use for checking conditions
	 * @param extraStates The extra states to consider for the condition
	 * @param attr The style attribute to get the value for
	 * @param withDefault Whether to use the attribute's default value if this style sheet does not have a value whose condition matches the
	 *        element and extra states instead of null
	 * @return The value of the given attribute in this style sheet whose condition matches the given element and extra states
	 */
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

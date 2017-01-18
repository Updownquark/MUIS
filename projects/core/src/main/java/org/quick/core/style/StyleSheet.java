package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.assoc.ObservableMultiMap;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;

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

	default ObservableMultiMap<AppliedStyleModifierProperty<?>, StyleConditionValue<?>> getModifierExpressions(StyleAttribute<?> attr) {
		// TODO will not be default when this arch is done
		return null;
	}

	/**
	 * @param <T> The type of the attribute
	 * @param condition The style condition to get attribute values for
	 * @param attr The attribute get values for
	 * @return All conditional values defined in this style sheet for the given attribute matching the given condition
	 */
	default <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleConditionInstance<?> condition,
		StyleAttribute<T> attr) {
		ObservableSortedSet<StyleConditionValue<T>> exprs = getStyleExpressions(attr);
		exprs = exprs.filterStatic(scv -> scv.getCondition().matchesType(condition.getElementType()));
		exprs = exprs.refreshEach(scv -> scv.getCondition().matches(condition)).filter(scv -> scv.getCondition().matches(condition).get());
		return exprs;
	}

	/**
	 * @param condition The style condition to use
	 * @param attr The attribute to check
	 * @return Whether this style sheet has any values for the given attribute for which the condition is met for the given condition
	 *         instance
	 */
	default boolean isSet(StyleConditionInstance<?> condition, StyleAttribute<?> attr) {
		for (StyleConditionValue<?> scv : getStyleExpressions(attr))
			if (scv.getCondition().matches(condition).get())
				return true;
		return false;
	}

	/**
	 * @param <T> The type of the attribute
	 * @param condition The condition instance to get the best match
	 * @param attr The attribute to get the best condition value for
	 * @return The condition value whose condition most closely matches the given condition instance
	 */
	default <T> ObservableValue<StyleConditionValue<T>> getBestMatch(StyleConditionInstance<?> condition, StyleAttribute<T> attr) {
		return getStyleExpressions(condition, attr).getFirst();
	}

	/**
	 * @param <T> The type of the attribute
	 * @param condition The style condition to use
	 * @param attr The style attribute to get the value for
	 * @param withDefault Whether to use the attribute's default value if this style sheet does not have a value whose condition matches the
	 *        condition instance instead of null
	 * @return The value of the given attribute in this style sheet whose condition matches the given condition instance
	 */
	default <T> ObservableValue<T> get(StyleConditionInstance<?> condition, StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<StyleConditionValue<T>> exprs = getBestMatch(condition, attr);

		ObservableValue<T> value;
		if (withDefault)
			value = ObservableValue.flatten(exprs, () -> attr.getDefault());
		else
			value = ObservableValue.flatten(exprs);
		return value;
	}
}

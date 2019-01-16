package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.QuickElement;

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
	 * @param condition The style condition to use
	 * @param attr The attribute to check
	 * @return Whether this style sheet has any values for the given attribute for which the condition is met for the given condition
	 *         instance
	 */
	default boolean isSet(StyleConditionInstance<?> condition, StyleAttribute<?> attr) {
		for (StyleConditionValue<?> scv : getStyleExpressions(attr))
			if (scv.getCondition().currentlyMatches(condition))
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
		return getStyleExpressions(attr).observeFind(scv -> scv.getCondition().currentlyMatches(condition)).first()
			.refresh(condition.changes()).find();
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

	/**
	 * @param element The element to use for checking conditions
	 * @param attr The attribute to check
	 * @return Whether this style sheet has any values for the given attribute for which the condition is met for the given element
	 */
	default boolean isSet(QuickElement element, StyleAttribute<?> attr) {
		return isSet(StyleConditionInstance.of(element), attr);
	}

	/**
	 * @param <T> The type of the attribute
	 * @param element The element to get the best match
	 * @param attr The attribute to get the best condition value for
	 * @return The condition value whose condition most closely matches the given element's state
	 */
	default <T> ObservableValue<StyleConditionValue<T>> getBestMatch(QuickElement element, StyleAttribute<T> attr) {
		return getBestMatch(StyleConditionInstance.of(element), attr);
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
		return get(StyleConditionInstance.of(element), attr, withDefault);
	}
}

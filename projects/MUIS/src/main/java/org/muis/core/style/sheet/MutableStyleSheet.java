package org.muis.core.style.sheet;

import org.muis.core.MuisElement;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.stateful.StateExpression;

/** A {@link StyleSheet} that can be modified directly */
public interface MutableStyleSheet extends StyleSheet {
	/**
	 * Sets the value of an attribute, potentially in a group-, type-, or state-specific way
	 *
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param groupName The name of the group for the value to be active for
	 * @param type The type of element for the value to be active for
	 * @param exp The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	<T> void set(StyleAttribute<T> attr, String groupName, Class<? extends MuisElement> type, StateExpression exp, T value)
		throws IllegalArgumentException;

	/**
	 * Clears the potentially group-, type-, or state-specific value of an attribute
	 *
	 * @param attr The attribute to clear the value of
	 * @param groupName The name of the group to clear the value for
	 * @param type The type of element to clear the value for
	 * @param exp The state expression to clear the value for
	 */
	void clear(StyleAttribute<?> attr, String groupName, Class<? extends MuisElement> type, StateExpression exp);
}

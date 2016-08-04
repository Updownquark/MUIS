package org.quick.core.style2;

import java.util.ArrayList;

import org.quick.core.style.StyleAttribute;

/** A StyleSheet whose conditional values can be directly set */
public interface MutableStyleSheet extends StyleSheet, ConditionalStyleSetter {
	/**
	 * Clears a conditional value of a given attribute in this style sheet, if set
	 * 
	 * @param attr The attribute to clear the value of
	 * @param condition The condition to clear the attribute's value for
	 * @return This style sheet
	 */
	MutableStyleSheet clear(StyleAttribute<?> attr, StyleCondition condition);

	/**
	 * Clears all conditional values of a given attribute in this style sheet
	 *
	 * @param <T> The type of the attribute to clear
	 * @param attr The attribute to clear the value of
	 * @return This style sheet
	 */
	default <T> MutableStyleSheet clear(StyleAttribute<T> attr) {
		for(StyleConditionValue<T> scv : new ArrayList<>(getStyleExpressions(attr))){
			clear(attr, scv.getCondition());
		}
		return this;
	}
}

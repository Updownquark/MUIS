package org.muis.core.style;

/** An extension of StatefulStyle that allows setting of attribute values */
public interface MutableStatefulStyle extends StatefulStyle, MutableStyle {
	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param expr The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	<T> void set(StyleAttribute<T> attr, StateExpression expr, T value);

	/**
	 * @param attr The attribute to clear the value of
	 * @param expr The state expression to clear the value for
	 */
	void clear(StyleAttribute<?> attr, StateExpression expr);
}

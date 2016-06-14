package org.quick.core.style;

import org.observe.ObservableValue;

/**
 * An extension of CondtionalStyle that allows setting of attribute values
 *
 * @param <S> The type of style set
 * @param <E> The type of expression supported by the style set
 */
public interface MutableConditionalStyle<S extends ConditionalStyle<S, E>, E extends StyleExpression<E>> extends ConditionalStyle<S, E> {
	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param expr The expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	<T> MutableConditionalStyle<S, E> set(StyleAttribute<T> attr, E expr, ObservableValue<T> value);

	/**
	 * @param attr The attribute to clear the value of
	 * @param expr The expression to clear the value for
	 * @return This style, for chaining
	 */
	MutableConditionalStyle<S, E> clear(StyleAttribute<?> attr, E expr);
}

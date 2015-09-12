package org.quick.core.style;

/** An extension of QuickStyle that allows setting of attribute values */
public interface MutableStyle extends QuickStyle {
	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 * @return This style, for chaining
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	<T> MutableStyle set(StyleAttribute<T> attr, T value) throws IllegalArgumentException;

	/**
	 * @param attr The attribute to clear the value of
	 * @return This style, for chaining
	 */
	MutableStyle clear(StyleAttribute<?> attr);
}

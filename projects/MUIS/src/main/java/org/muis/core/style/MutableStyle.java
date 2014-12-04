package org.muis.core.style;

/** An extension of MuisStyle that allows setting of attribute values */
public interface MutableStyle extends MuisStyle {
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

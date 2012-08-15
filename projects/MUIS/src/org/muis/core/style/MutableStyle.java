package org.muis.core.style;

/**
 * An extension of MuisStyle that allows setting of attribute values. If this style is stateful, the modification methods apply to the base
 * state--they will be effective if no stateful settings override them.
 */
public interface MutableStyle extends MuisStyle {
	/**
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	<T> void set(StyleAttribute<T> attr, T value) throws IllegalArgumentException;

	/** @param attr The attribute to clear the value of */
	void clear(StyleAttribute<?> attr);
}

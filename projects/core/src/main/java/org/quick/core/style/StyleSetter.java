package org.quick.core.style;

import org.observe.ObservableValue;

/** Accepts a value for a style attribute */
public interface StyleSetter {
	/**
	 * @param <T> The type of the attribute
	 * @param attr The style attribute to set the value of
	 * @param value The value for the attribute
	 * @return This setter
	 */
	<T> StyleSetter set(StyleAttribute<T> attr, ObservableValue<? extends T> value);

	/**
	 * Sets a constant value on a style attribute
	 *
	 * @param <T> The compile-time type of the attribute
	 * @param attr The style attribute to set the value for
	 * @param value The value to set for the attribute
	 * @return This setter
	 */
	default <T> StyleSetter setConstant(StyleAttribute<T> attr, T value) {
		return set(attr, ObservableValue.of(attr.getType().getType(), value));
	}
}

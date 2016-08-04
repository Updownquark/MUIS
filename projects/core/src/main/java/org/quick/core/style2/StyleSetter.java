package org.quick.core.style2;

import org.observe.ObservableValue;
import org.quick.core.style.StyleAttribute;

/** Accepts a value for a style attribute */
public interface StyleSetter {
	/**
	 * @param <T> The type of the attribute
	 * @param attr The style attribute to set the value of
	 * @param value The value for the attribute
	 * @return This setter
	 */
	<T> StyleSetter set(StyleAttribute<T> attr, ObservableValue<? extends T> value);
}

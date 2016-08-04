package org.quick.core.style2;

import org.observe.ObservableValue;
import org.quick.core.style.StyleAttribute;

/** A QuickStyle that can be directly modified */
public interface MutableStyle extends QuickStyle, StyleSetter {
	@Override
	<T> MutableStyle set(StyleAttribute<T> attr, ObservableValue<? extends T> value);

	/**
	 * Clears the value of an attribute in this style sheet
	 * 
	 * @param attr The attribute to clear the value of
	 * @return This style
	 */
	MutableStyle clear(StyleAttribute<?> attr);
}

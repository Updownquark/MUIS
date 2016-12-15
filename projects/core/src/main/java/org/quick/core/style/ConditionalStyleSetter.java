package org.quick.core.style;

import org.observe.ObservableValue;
import org.quick.core.QuickElement;

/** Accepts conditional style values */
public interface ConditionalStyleSetter extends StyleSetter {
	@Override
	default <T> StyleSetter set(StyleAttribute<T> attr, ObservableValue<? extends T> value) {
		return set(attr, StyleCondition.build(QuickElement.class).build(), value);
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The style attribute whose value to set
	 * @param condition The condition for which the value is to be valid
	 * @param value The value for the style attribute
	 * @return This setter
	 */
	<T> StyleSetter set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value);
}

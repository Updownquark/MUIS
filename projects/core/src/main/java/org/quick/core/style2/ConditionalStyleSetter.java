package org.quick.core.style2;

import org.observe.ObservableValue;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;

public interface ConditionalStyleSetter extends StyleSetter {
	@Override
	default <T> StyleSetter set(StyleAttribute<T> attr, ObservableValue<? extends T> value) {
		return set(attr, StyleCondition.build(QuickElement.class).build(), value);
	}

	<T> StyleSetter set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value);
}

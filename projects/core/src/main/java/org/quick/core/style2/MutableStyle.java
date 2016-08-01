package org.quick.core.style2;

import org.observe.ObservableValue;
import org.quick.core.style.StyleAttribute;

public interface MutableStyle extends QuickStyle, StyleSetter {
	@Override
	<T> MutableStyle set(StyleAttribute<T> attr, ObservableValue<? extends T> value);

	MutableStyle clear(StyleAttribute<?> attr);
}

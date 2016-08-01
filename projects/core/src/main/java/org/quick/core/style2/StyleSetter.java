package org.quick.core.style2;

import org.observe.ObservableValue;
import org.quick.core.style.StyleAttribute;

public interface StyleSetter {
	<T> StyleSetter set(StyleAttribute<T> attr, ObservableValue<? extends T> value);
}

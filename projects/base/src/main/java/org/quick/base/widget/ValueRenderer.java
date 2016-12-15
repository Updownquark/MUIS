package org.quick.base.widget;

import org.observe.ObservableValue;
import org.quick.core.QuickElement;

public abstract class ValueRenderer<V> extends QuickElement {
	public abstract void renderFor(ObservableValue<? extends V> value, boolean selected, boolean focused);
}

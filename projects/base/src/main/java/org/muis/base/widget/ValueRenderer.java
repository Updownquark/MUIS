package org.muis.base.widget;

import org.muis.core.MuisElement;
import org.observe.ObservableValue;

public abstract class ValueRenderer<V> extends MuisElement {
	public abstract void renderFor(ObservableValue<? extends V> value, boolean selected, boolean focused);
}

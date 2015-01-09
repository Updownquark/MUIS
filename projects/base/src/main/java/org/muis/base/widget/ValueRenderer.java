package org.muis.base.widget;

import org.muis.core.MuisElement;

public abstract class ValueRenderer<V> extends MuisElement {
	public abstract void renderFor(V value, boolean selected);
}

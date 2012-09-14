package org.muis.core.style;

import org.muis.core.MuisElement;

/** A default dependency-less, mutable implementation of {@link StyleSheet} */
public class DefaultStyleSheet extends SimpleStyleSheet implements MutableStyleSheet {
	@Override
	public StyleSheet [] getStyleSheetDependencies() {
		return new StyleSheet[0];
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		return allLocal();
	}

	@Override
	public <T> StyleGroupTypeExpressionValue<?, T> [] getExpressions(StyleAttribute<T> attr) {
		return getLocalExpressions(attr);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, String groupName, Class<? extends MuisElement> type, StateExpression exp, T value)
		throws IllegalArgumentException {
		super.set(attr, groupName, type, exp, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr, String groupName, Class<? extends MuisElement> type, StateExpression exp) {
		super.clear(attr, groupName, type, exp);
	}
}

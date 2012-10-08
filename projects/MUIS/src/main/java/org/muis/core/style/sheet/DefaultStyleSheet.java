package org.muis.core.style.sheet;

import org.muis.core.style.StyleAttribute;

/** A default dependency-less, mutable implementation of {@link StyleSheet} */
public class DefaultStyleSheet extends SimpleStyleSheet implements MutableStyleSheet {
	@Override
	public StyleSheet [] getConditionalDependencies() {
		return new StyleSheet[0];
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		return allLocal();
	}

	@Override
	public <T> org.muis.core.style.StyleExpressionValue<StateGroupTypeExpression<?>, T> [] getExpressions(StyleAttribute<T> attr) {
		return getLocalExpressions(attr);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value)
		throws IllegalArgumentException {
		super.set(attr, exp, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
		super.clear(attr, exp);
	}
}

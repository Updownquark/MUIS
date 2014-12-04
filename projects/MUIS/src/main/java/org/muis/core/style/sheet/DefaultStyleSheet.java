package org.muis.core.style.sheet;

import org.muis.core.rx.ObservableList;
import org.muis.core.style.StyleAttribute;

/** A default dependency-less, mutable implementation of {@link StyleSheet} */
public class DefaultStyleSheet extends SimpleStyleSheet implements MutableStyleSheet {
	@Override
	public ObservableList<StyleSheet> getConditionalDependencies() {
		return (ObservableList<StyleSheet>) org.muis.core.rx.Observable.empty;
	}

	@Override
	public <T> DefaultStyleSheet set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
		IllegalArgumentException {
		super.set(attr, exp, value);
		return this;
	}

	@Override
	public DefaultStyleSheet clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
		super.clear(attr, exp);
		return this;
	}
}

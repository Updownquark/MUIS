package org.quick.core.style.sheet;

import org.observe.ObservableValue;
import org.observe.collect.ObservableList;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.SimpleConditionalStyle;
import org.quick.core.style.StyleAttribute;

/** A default dependency-less, mutable implementation of {@link StyleSheet} */
public class DefaultStyleSheet extends SimpleStyleSheet implements MutableStyleSheet {
	/** @see SimpleConditionalStyle#SimpleConditionalStyle(QuickMessageCenter) */
	protected DefaultStyleSheet(QuickMessageCenter msg) {
		super(msg);
	}

	@Override
	public ObservableList<StyleSheet> getConditionalDependencies() {
		return (ObservableList<StyleSheet>) org.observe.Observable.empty;
	}

	@Override
	public <T> DefaultStyleSheet set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
		return this;
	}

	@Override
	public DefaultStyleSheet clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
		super.clear(attr, exp);
		return this;
	}
}

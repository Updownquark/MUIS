package org.quick.core.style.sheet;

import org.observe.ObservableValue;
import org.observe.collect.ObservableList;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.StyleAttribute;

/** An AnimatedStyleSheet whose modification methods are exposed */
public class MutableAnimatedStyleSheet extends AnimatedStyleSheet {
	/** @param depends The style sheets that this style sheet inherits style information from */
	public MutableAnimatedStyleSheet(QuickMessageCenter msg, ObservableList<StyleSheet> depends) {
		super(msg, depends);
	}

	@Override
	public void addVariable(AnimatedVariable var) {
		super.addVariable(var);
	}

	@Override
	public void removeVariable(AnimatedVariable var) {
		super.removeVariable(var);
	}

	@Override
	public void startAnimation() {
		super.startAnimation();
	}

	@Override
	public void stopAnimation() {
		super.stopAnimation();
	}

	@Override
	public <T> MutableAnimatedStyleSheet set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
		return this;
	}

	@Override
	public <T> MutableAnimatedStyleSheet set(StyleAttribute<T> attr, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
		return this;
	}

	@Override
	public MutableAnimatedStyleSheet clear(StyleAttribute<?> attr) {
		super.clear(attr);
		return this;
	}

	@Override
	public MutableAnimatedStyleSheet clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
		super.clear(attr, exp);
		return this;
	}
}

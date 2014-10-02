package org.muis.core.style.sheet;

import org.muis.core.model.MuisValueReferenceParser;
import org.muis.core.parser.MuisParseException;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.StyleAttribute;

/** An AnimatedStyleSheet whose modification methods are exposed */
public class MutableAnimatedStyleSheet extends AnimatedStyleSheet {
	/** @param modelParser The model parser for this style sheet to use */
	public MutableAnimatedStyleSheet(MuisValueReferenceParser modelParser) {
		super(modelParser);
	}

	@Override
	public MuisValueReferenceParser getModelParser() {
		return super.getModelParser();
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
	public <T> void setAnimatedValue(StyleAttribute<T> attr, StateGroupTypeExpression<?> expr, ObservableValue<? extends T> value) {
		super.setAnimatedValue(attr, expr, value);
	}

	@Override
	public void setAnimatedValue(StyleAttribute<?> attr, StateGroupTypeExpression<?> expr, String parseableValue) throws MuisParseException {
		super.setAnimatedValue(attr, expr, parseableValue);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateGroupTypeExpression<?> exp, T value) throws ClassCastException,
		IllegalArgumentException {
		super.set(attr, exp, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr) {
		super.clear(attr);
	}

	@Override
	public void clear(StyleAttribute<?> attr, StateGroupTypeExpression<?> exp) {
		super.clear(attr, exp);
	}
}

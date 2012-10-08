package org.muis.core.style.sheet;

import org.muis.core.style.StyleAttribute;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.ParsedItem;

/** An AnimatedStyleSheet whose modification methods are exposed */
public class MutableAnimatedStyleSheet extends AnimatedStyleSheet {
	/** Creates this animated style sheet with a default, empty environment */
	public MutableAnimatedStyleSheet() {
		this(new prisms.lang.DefaultEvaluationEnvironment());
	}

	/** @param env The evaluation environment for this style sheet to use */
	public MutableAnimatedStyleSheet(EvaluationEnvironment env) {
		super(env);
	}

	@Override
	public EvaluationEnvironment getEvaluationEnvironment() {
		return super.getEvaluationEnvironment();
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
	public void setAnimatedValue(StyleAttribute<?> attr, StateGroupTypeExpression<?> expr, ParsedItem value) {
		super.setAnimatedValue(attr, expr, value);
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

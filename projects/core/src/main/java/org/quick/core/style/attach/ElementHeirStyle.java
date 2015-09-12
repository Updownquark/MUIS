package org.quick.core.style.attach;

import java.util.List;

import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;
import org.quick.core.QuickElement;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.stateful.AbstractInternallyStatefulStyle;
import org.quick.core.style.stateful.MutableStatefulStyle;
import org.quick.core.style.stateful.StateExpression;
import org.quick.core.style.stateful.StatefulStyle;

/** Represents a set of style attributes that apply to all an element's descendants but not to the element itself */
public class ElementHeirStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle {
	private final ElementStyle theElStyle;

	private final List<StatefulStyle> theDependencyController;

	/** @param elStyle The element style that this heir style is for */
	public ElementHeirStyle(ElementStyle elStyle) {
		super(ObservableUtils.control(new ObservableArrayList<>(new prisms.lang.Type(StatefulStyle.class))), elStyle.getElement().state()
			.activeStates());
		theDependencyController = ObservableUtils.getController(getConditionalDependencies());
		theElStyle = elStyle;
		theDependencyController.add(elStyle);
	}

	/** @return The element style that this heir style depends on */
	public ElementStyle getElementStyle() {
		return theElStyle;
	}

	@Override
	public QuickElement getElement() {
		return theElStyle.getElement();
	}

	@Override
	public <T> ElementHeirStyle set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
		return this;
	}

	@Override
	public <T> ElementHeirStyle set(StyleAttribute<T> attr, StateExpression exp, T value) throws ClassCastException,
		IllegalArgumentException {
		super.set(attr, exp, value);
		return this;
	}

	@Override
	public ElementHeirStyle clear(StyleAttribute<?> attr) {
		super.clear(attr);
		return this;
	}

	@Override
	public ElementHeirStyle clear(StyleAttribute<?> attr, StateExpression exp) {
		super.clear(attr, exp);
		return this;
	}

	@Override
	public String toString() {
		return "style.heir of " + theElStyle.getElement();
	}
}

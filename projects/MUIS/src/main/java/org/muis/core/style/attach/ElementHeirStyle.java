package org.muis.core.style.attach;

import java.util.List;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.DefaultObservableList;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;
import org.muis.core.style.stateful.StatefulStyle;

/** Represents a set of style attributes that apply to all an element's descendants but not to the element itself */
public class ElementHeirStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle {
	private final ElementStyle theElStyle;

	private final List<StatefulStyle> theDependencyController;

	/** @param elStyle The element style that this heir style is for */
	public ElementHeirStyle(ElementStyle elStyle) {
		super(new DefaultObservableList<>(new prisms.lang.Type(StatefulStyle.class)));
		theDependencyController = ((DefaultObservableList<StatefulStyle>) getConditionalDependencies()).control(null);
		theElStyle = elStyle;
		theDependencyController.add(elStyle);
	}

	/** @return The element style that this heir style depends on */
	public ElementStyle getElementStyle() {
		return theElStyle;
	}

	/* Overridden to enable access by ElementStyle */
	@Override
	protected void setState(MuisState... newState) {
		super.setState(newState);
	}

	/* Overridden to enable access by ElementStyle */
	@Override
	protected void addState(MuisState state) {
		super.addState(state);
	}

	/* Overridden to enable access by ElementStyle */
	@Override
	protected void removeState(MuisState state) {
		super.removeState(state);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateExpression exp, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr) {
		super.clear(attr);
	}

	@Override
	public void clear(StyleAttribute<?> attr, StateExpression exp) {
		super.clear(attr, exp);
	}

	@Override
	public String toString() {
		return "style.heir of " + theElStyle.getElement();
	}
}

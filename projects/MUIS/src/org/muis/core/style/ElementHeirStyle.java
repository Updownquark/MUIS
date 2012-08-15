package org.muis.core.style;

/** Represents a set of style attributes that apply to all an element's descendants but not to the element itself */
public class ElementHeirStyle extends AbstractStatefulStyle implements MutableStatefulStyle {
	private final ElementStyle theElStyle;

	/** @param elStyle The element style that this heir style is for */
	public ElementHeirStyle(ElementStyle elStyle) {
		theElStyle = elStyle;
		addDependency(elStyle);
	}

	/** @return The element style that this heir style depends on */
	public ElementStyle getParent() {
		return theElStyle;
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, T value) throws IllegalArgumentException {
		super.set(attr, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateExpression exp, T value) throws IllegalArgumentException {
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
}

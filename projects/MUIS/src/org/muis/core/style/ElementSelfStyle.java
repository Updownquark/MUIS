package org.muis.core.style;

/** Represents a style set that applies only to a particular element and not to its descendants */
public class ElementSelfStyle extends AbstractStatefulStyle implements MutableStatefulStyle {
	private final ElementStyle theElStyle;

	/** @param elStyle The element style that this self style is for */
	public ElementSelfStyle(ElementStyle elStyle) {
		theElStyle = elStyle;
		addDependency(elStyle);
	}

	/** @return The element style that depends on this self-style */
	public ElementStyle getElementStyle() {
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

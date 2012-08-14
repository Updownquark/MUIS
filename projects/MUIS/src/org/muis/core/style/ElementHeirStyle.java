package org.muis.core.style;

/** Represents a set of style attributes that apply to all an element's descendants but not to the element itself */
public class ElementHeirStyle extends AbstractMuisStyle {
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
}

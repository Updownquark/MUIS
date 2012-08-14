package org.muis.core.style;

/** Represents a style set that applies only to a particular element and not to its descendants */
public class ElementSelfStyle extends AbstractMuisStyle {
	private final ElementStyle theElStyle;

	/** @param elStyle The element style that this self style is for */
	public ElementSelfStyle(ElementStyle elStyle) {
		theElStyle = elStyle;
	}

	/** @return The element style that depends on this self-style */
	public ElementStyle getElementStyle() {
		return theElStyle;
	}
}

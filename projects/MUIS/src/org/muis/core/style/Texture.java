package org.muis.core.style;

/** A texture is an object that renders a surface with some kind of graphic on top of the already rendered background */
public interface Texture
{
	/**
	 * Renders this texture
	 *
	 * @param graphics The graphics to render the texture on top of
	 * @param element The element to render the texture for. Normally this parameter will only be used for size information.
	 * @param area The area over which rendering is needed
	 */
	void render(java.awt.Graphics2D graphics, org.muis.core.MuisElement element, java.awt.Rectangle area);
}

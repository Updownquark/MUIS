package org.quick.widget.core.style;

import java.awt.Graphics2D;

import org.quick.core.Rectangle;
import org.quick.core.style.Texture;
import org.quick.widget.core.QuickWidget;

public interface QuickWidgetTexture extends Texture {
	/**
	 * Renders this texture
	 *
	 * @param graphics The graphics to render the texture on top of
	 * @param widget The widget to render the texture for. Normally this parameter will only be used for size information.
	 * @param area The area over which rendering is needed
	 */
	void render(Graphics2D graphics, QuickWidget widget, Rectangle area);
}

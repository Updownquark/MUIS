package org.muis.core.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.MuisElement;

/**
 * Renders the background color of the widget over its entire area, minus what the {@link BackgroundStyles#cornerRadius} specifies to leave
 * unpainted
 */
public class BaseTexture implements Texture
{
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area)
	{
		Color orig = graphics.getColor();
		try
		{
			graphics.setColor(org.muis.core.MuisUtils.getBackground(element.getStyle()));
			// TODO Implement BackgroundStyles.cornerRadius
			int x = area == null ? 0 : area.x;
			int y = area == null ? 0 : area.y;
			int w = area == null ? element.getWidth() : (area.width < element.getWidth() ? area.width : element.getWidth());
			int h = area == null ? element.getHeight() : (area.height < element.getHeight() ? area.height : element.getHeight());
			graphics.fillRect(x, y, w, h);
		} finally
		{
			graphics.setColor(orig);
		}
	}
}

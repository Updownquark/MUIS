package org.muis.core.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.MuisElement;

/** Renders the background style of the widget */
public class BaseTexture implements Texture
{
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area)
	{
		int w = element.getWidth();
		int h = element.getHeight();
		int x = area == null ? 0 : area.x;
		int y = area == null ? 0 : area.y;
		int renderW = area == null ? element.getWidth() : (area.width < w ? area.width : w);
		int renderH = area == null ? element.getHeight() : (area.height < h ? area.height : h);
		Color orig = graphics.getColor();
		try
		{
			graphics.setColor(org.muis.core.MuisUtils.getBackground(element.getStyle()));
			Size cornerRad = element.getStyle().get(BackgroundStyles.cornerRadius);
			int wRad = cornerRad.evaluate(w);
			int hRad = cornerRad.evaluate(h);
			int wRad2 = wRad * 2;
			int hRad2 = hRad * 2;
			if(wRad >= hRad)
			{
				if(y >= hRad && y + renderH <= h - hRad)
					graphics.fillRect(x, y, w, h);
				else
				{
					graphics.fillRect(x, y + hRad, w, h - hRad2);
					// Left side
					if(x < wRad)
					{
						// Upper left corner
						if(y < hRad)
							graphics.fillOval(0, 0, wRad2, hRad2);
						// Lower left corner
						if(y + renderH > h - hRad)
							graphics.fillOval(0, h - hRad2, wRad2, hRad2);
					}
					// Right side
					if(x + renderW > w - wRad)
					{
						// Upper right corner
						if(y < hRad)
							graphics.fillOval(w - wRad2, 0, wRad2, hRad2);
						// Lower right corner
						if(y + renderH > h - hRad)
							graphics.fillOval(w - wRad2, h - hRad2, wRad2, hRad2);
					}
					graphics.fillRect(wRad, 0, w - wRad2, hRad);
					graphics.fillRect(wRad, h - hRad, w - wRad2, hRad);
				}
			}
			else
			{
				if(x >= wRad && x + renderW <= w - wRad)
					graphics.fillRect(x, y, w, h);
				else
				{
					graphics.fillRect(x + wRad, y, w - wRad2, h);
					// Top side
					if(y < hRad)
					{
						// Upper left corner
						if(x < wRad)
							graphics.fillOval(0, 0, wRad2, hRad2);
						// Upper right corner
						if(x + renderW > w - wRad)
							graphics.fillOval(w - wRad2, 0, wRad2, hRad2);
					}
					// Bottom side
					if(y + renderH > h - hRad)
					{
						// Lower left corner
						if(x < wRad)
							graphics.fillOval(0, h - hRad2, wRad2, hRad2);
						// Lower right corner
						if(x + renderW > w - wRad)
							graphics.fillOval(w - wRad2, h - hRad2, wRad2, hRad2);
					}
					graphics.fillRect(0, hRad, wRad, h - hRad2);
					graphics.fillRect(w - wRad, hRad, wRad, h - hRad2);
				}
			}
			// TODO Implement BackgroundStyles.cornerRadius
		} finally
		{
			graphics.setColor(orig);
		}
	}
}

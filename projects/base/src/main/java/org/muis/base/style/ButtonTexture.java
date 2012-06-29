package org.muis.base.style;

import java.awt.Color;

import org.muis.core.MuisElement;

/** Renders a button-looking texture over an element */
public class ButtonTexture implements org.muis.core.style.Texture
{
	@Override
	public void render(java.awt.Graphics2D graphics, MuisElement element, java.awt.Rectangle area)
	{
		int w = element.getWidth();
		int h = element.getHeight();
		int startX = area == null ? 0 : area.x;
		int startY = area == null ? 0 : area.y;
		int endX = area == null ? w : startX + area.width;
		int endY = area == null ? h : startY + area.height;
		Color orig = graphics.getBackground();
		int radius = element.getStyle().get(ButtonStyles.radius);
		float source = element.getStyle().get(org.muis.core.style.TextureStyle.lightSource);
		float sin = (float) Math.sin(source * Math.PI / 180);
		float cos = (float) Math.cos(source * Math.PI / 180);

		// Find the intersection of the ray from the center of the rectangle to the light source with the border
		boolean left = source >= 180;
		boolean top = source <= 90 || source > 270;
		if(left)
		{
			source -= 180;
			if(top)
				source -= 90;
		}
		else if(!top)
			source -= 90;
		try
		{
			/*for(int i = 0; i < radius; i++)
			{
				float brighten;
				// Vertical edges
				int lineStartY = i;
				int lineEndY = h - i;
				if(lineStartY < startY)
					lineStartY = startY;
				if(lineEndY > endY)
					lineEndY = endY;
				if(lineStartY < lineEndY)
				{
					int x = i;
					// Left edge
					if(x >= startX || x < endX)
					{
						brighten = -sin * (radius - i) / radius;
						if(brighten > 0)
							graphics.setBackground(new Color(255, 255, 255, (int) (brighten * 255)));
						else
							graphics.setBackground(new Color(0, 0, 0, (int) (-brighten * 255)));
						graphics.drawLine(x, lineStartY, x, lineEndY);
					}
					x = w - i - 1;
					// Right edge
					if(x >= startX || x < endX)
					{
						brighten = sin * (radius - i) / radius;
						if(brighten > 0)
							graphics.setBackground(new Color(255, 255, 255, (int) (brighten * 255)));
						else
							graphics.setBackground(new Color(0, 0, 0, (int) (-brighten * 255)));
						graphics.drawLine(x, lineStartY, x, lineEndY);
					}
				}
				int lineStartX = i;
				int lineEndX = w - i;
				if(lineStartX < startX)
					lineStartX = startX;
				if(lineEndX > endX)
					lineEndX = endX;
				if(lineStartX < lineEndX)
				{
					int y = i;
					// Top edge
					if(y >= startY || y < endY)
					{
						brighten = cos * (radius - i) / radius;
						if(brighten > 0)
							graphics.setBackground(new Color(255, 255, 255, (int) (brighten * 255)));
						else
							graphics.setBackground(new Color(0, 0, 0, (int) (-brighten * 255)));
						graphics.drawLine(lineStartX, y, lineEndX, y);
					}
					y = h - i - 1;
					// Right edge
					if(y >= startY || y < endY)
					{
						brighten = -cos * (radius - i) / radius;
						if(brighten > 0)
							graphics.setBackground(new Color(255, 255, 255, (int) (brighten * 255)));
						else
							graphics.setBackground(new Color(0, 0, 0, (int) (-brighten * 255)));
						graphics.drawLine(lineStartX, y, lineEndX, y);
					}
				}
			}*/
			for(int y = startY; y < endY; y++)
				for(int x = startX; x < endX; x++)
				{
					float brighten = getBrighten(x, y, w, h, radius, sin, cos);
					if(brighten < 0)
					{
						graphics.setColor(new Color(0, 0, 0, (int) (-brighten * 255)));
						graphics.drawRect(x, y, 1, 1);
					}
					else if(brighten > 0)
					{
						graphics.setColor(new Color(255, 255, 255, (int) (brighten * 255)));
						graphics.drawRect(x, y, 1, 1);
					}
				}
		} finally
		{
			graphics.setBackground(orig);
		}
	}

	/** @param x The x-value of the pixel to brighten or darken
	 * @param y The y-value of the pixel to brighten or darken
	 * @param w The width of the element being rendered
	 * @param h The height of the element being rendered
	 * @param radius The button radius to render with
	 * @param sinSource The sin of the light source angle from the vertical
	 * @param cosSource The cosine of the light source angle from the vertical
	 * @return A number between -1 (to darken completely) and 1 (to wash out completely) to lighten or darken the background color */
	public float getBrighten(int x, int y, int w, int h, int radius, float sinSource, float cosSource)
	{
		int minDist;
		minDist = x;
		boolean horizEdge = true;
		boolean negOrPos = false;
		if(w - x < minDist)
		{
			minDist = w - x;
			negOrPos = true;
		}
		if(y < minDist)
		{
			minDist = y;
			horizEdge = false;
			negOrPos = false;
		}
		if(h - y < minDist)
		{
			minDist = h - y;
			horizEdge = false;
			negOrPos = true;
		}
		if(minDist > radius)
			return 0;

		if(!horizEdge)
		{ // Top or bottom edge
			if(!negOrPos) // Top edge
				return cosSource * (radius - minDist) / radius;
			else
				// Bottom edge
				return -cosSource * (radius - minDist) / radius;
		}
		else
		{
			if(!negOrPos) // Left edge
				return -sinSource * (radius - minDist) / radius;
			else
				// Right edge
				return sinSource * (radius - minDist) / radius;
		}
	}
}

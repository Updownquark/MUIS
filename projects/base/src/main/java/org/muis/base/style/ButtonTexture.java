package org.muis.base.style;

import java.awt.Color;

import org.muis.core.MuisDocument;

/** Renders a button-looking texture over an element */
public class ButtonTexture extends org.muis.core.style.BaseTexture
{
	private static class CornerRender
	{
		private int [][] theLightAlphas;

		private int [][] theShadowAlphas;

		CornerRender(int radius)
		{
			radius++;
			theLightAlphas = new int[radius][radius];
			theShadowAlphas = new int[radius][radius];
		}

		void render(float lightSource)
		{
			// TODO
		}

		int getRadius()
		{
			return theLightAlphas.length - 1;
		}

		int getLightAlpha(int x, int y)
		{
			return theLightAlphas[y][x];
		}

		int getShadowAlpha(int x, int y)
		{
			return theShadowAlphas[y][x];
		}
	}

	private static class CornerRenderKey
	{
		final float source;

		final int radius;

		CornerRenderKey(float src, int rad)
		{
			source = src;
			radius = rad;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof CornerRenderKey && Math.abs(((CornerRenderKey) o).source - source) <= 1; // 1 degree doesn't matter
		}

		@Override
		public int hashCode()
		{
			return Float.floatToIntBits(source);
		}
	}

	private static org.muis.core.MuisCache.CacheItemType<CornerRenderKey, CornerRender, RuntimeException> cornerRendering = new org.muis.core.MuisCache.CacheItemType<CornerRenderKey, CornerRender, RuntimeException>() {
		@Override
		public CornerRender generate(MuisDocument doc, CornerRenderKey key) throws RuntimeException
		{
			CornerRender ret = new CornerRender(key.radius);
			ret.render(key.source);
			return ret;
		}

		@Override
		public int size(CornerRender value)
		{
			return value.getRadius() * value.getRadius() * 2 + 16;
		}
	};

	@Override
	public void render(java.awt.Graphics2D graphics, org.muis.core.MuisElement element, java.awt.Rectangle area)
	{
		super.render(graphics, element, area);
		int w = element.getWidth();
		int h = element.getHeight();
		// int startX = area == null ? 0 : area.x;
		// int startY = area == null ? 0 : area.y;
		// int endX = area == null ? w : startX + area.width;
		// int endY = area == null ? h : startY + area.height;
		org.muis.core.style.Size radius = element.getStyle().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		int wRad = radius.evaluate(w);
		int hRad = radius.evaluate(h);
		if(wRad * 2 > w)
			wRad = w / 2;
		if(hRad * 2 > h)
			hRad = h / 2;
		// The radius of the corner render we get needs to be either at least the width or height radius for good resolution.
		int maxRad = wRad;
		if(hRad > maxRad)
			maxRad = hRad;
		float source = element.getStyle().get(org.muis.core.style.LightedStyle.lightSource).floatValue();
		Color light = element.getStyle().get(org.muis.core.style.LightedStyle.lightColor);
		Color shadow = element.getStyle().get(org.muis.core.style.LightedStyle.shadowColor);
		java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(wRad, hRad, java.awt.image.BufferedImage.TYPE_4BYTE_ABGR);
		for(int i = 0; i < 4; i++)
		{
			// TODO test for area's containment of this corner, if area!=null
			float tempSource = source - 90 * i;
			while(tempSource >= 360)
				tempSource -= 360;
			CornerRenderKey key = new CornerRenderKey(tempSource, maxRad);
			CornerRender cr = element.getDocument().getCache().getAndWait(element.getDocument(), cornerRendering, key, true);
			if(cr.getRadius() < maxRad)
			{
				// Regenerate with a big enough radius
				element.getDocument().getCache().remove(cornerRendering, key);
				cr = element.getDocument().getCache().getAndWait(element.getDocument(), cornerRendering, key, true);
			}

			for(int x = 0; x < wRad; x++)
				for(int y = 0; y < hRad; y++)
				{
					int crX = x, crY = y;
					switch (i)
					{
					case 0: // Top left corner
						crX = x;
						crY = y;
						break;
					case 1: // Top right corner
						crX = wRad - x;
						crY = y;
						break;
					case 2: // Bottom right corner
						crX = wRad - x;
						crY = hRad - y;
						break;
					case 3: // Bottom left corner
						crX = x;
						crY = hRad - y;
						break;
					}
					crX = Math.round(crX * cr.getRadius() * 1.0f / wRad);
					crY = Math.round(crX * cr.getRadius() * 1.0f / hRad);
					int alpha = cr.getLightAlpha(crX, crY);
					if(alpha > 0)
						img.setRGB(x, y, light.getRGB() | (alpha << 24));
					else
					{
						alpha = cr.getShadowAlpha(crX, crY);
						if(alpha > 0)
							img.setRGB(x, y, shadow.getRGB() | (alpha << 24));
					}
				}
			int renderX = 0, renderY = 0;
			switch (i)
			{
			case 0:
				renderX = 0;
				renderY = 0;
				break;
			case 1:
				renderX = w - wRad;
				renderY = 0;
				break;
			case 2:
				renderX = w - wRad;
				renderY = h - hRad;
				break;
			case 3:
				renderX = 0;
				renderY = h - hRad;
				break;
			}
			// TODO make an image observer so we can be sure the graphics finish rendering for each corner before the next is drawn
			// Might be better than making a new buffered image each time the rendering doesn't finish
			if(!graphics.drawImage(img, renderX, renderY, wRad, hRad, 0, 0, wRad, hRad, null))
				img = new java.awt.image.BufferedImage(wRad, hRad, java.awt.image.BufferedImage.TYPE_4BYTE_ABGR);
		}
		// TODO Corners drawn, now draw lines

		/*float sin = (float) Math.sin(source * Math.PI / 180);
		float cos = (float) Math.cos(source * Math.PI / 180);

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
		for(int i = 0; i < radius; i++)
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
						graphics.setColor(new Color(255, 255, 255, (int) (brighten * 255)));
					else
						graphics.setColor(new Color(0, 0, 0, (int) (-brighten * 255)));
					graphics.drawLine(x, lineStartY, x, lineEndY);
				}
				x = w - i - 1;
				// Right edge
				if(x >= startX || x < endX)
				{
					brighten = sin * (radius - i) / radius;
					if(brighten > 0)
						graphics.setColor(new Color(255, 255, 255, (int) (brighten * 255)));
					else
						graphics.setColor(new Color(0, 0, 0, (int) (-brighten * 255)));
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
						graphics.setColor(new Color(255, 255, 255, (int) (brighten * 255)));
					else
						graphics.setColor(new Color(0, 0, 0, (int) (-brighten * 255)));
					graphics.drawLine(lineStartX, y, lineEndX, y);
				}
				y = h - i - 1;
				// Right edge
				if(y >= startY || y < endY)
				{
					brighten = -cos * (radius - i) / radius;
					if(brighten > 0)
						graphics.setColor(new Color(255, 255, 255, (int) (brighten * 255)));
					else
						graphics.setColor(new Color(0, 0, 0, (int) (-brighten * 255)));
					graphics.drawLine(lineStartX, y, lineEndX, y);
				}
			}
		}*/

		/*for(int y = startY; y < endY; y++)
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
			}*/
	}

	/**
	 * @param x The x-value of the pixel to brighten or darken
	 * @param y The y-value of the pixel to brighten or darken
	 * @param w The width of the element being rendered
	 * @param h The height of the element being rendered
	 * @param radius The button radius to render with
	 * @param sinSource The sin of the light source angle from the vertical
	 * @param cosSource The cosine of the light source angle from the vertical
	 * @return A number between -1 (to darken completely) and 1 (to wash out completely) to lighten or darken the background color
	 */
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

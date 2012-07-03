package org.muis.base.style;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

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

	private static class CornerRenderImage implements RenderedImage
	{
		private CornerRender theRender;

		private Color theLight;

		private Color theShadow;

		private int theWidth;

		private int theHeight;

		CornerRenderImage(CornerRender cr, Color light, Color shadow, int width, int height)
		{
		}

		@Override
		public java.util.Vector<RenderedImage> getSources()
		{
			return new java.util.Vector<>();
		}

		@Override
		public Object getProperty(String name)
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public String [] getPropertyNames()
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public java.awt.image.ColorModel getColorModel()
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public java.awt.image.SampleModel getSampleModel()
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public int getWidth()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getHeight()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getMinX()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getMinY()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getNumXTiles()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getNumYTiles()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getMinTileX()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getMinTileY()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getTileWidth()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getTileHeight()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getTileGridXOffset()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public int getTileGridYOffset()
		{
			return 0;
			// TODO Auto-generated method stub
		}

		@Override
		public Raster getTile(int tileX, int tileY)
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public Raster getData()
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public Raster getData(Rectangle rect)
		{
			return null;
			// TODO Auto-generated method stub
		}

		@Override
		public WritableRaster copyData(WritableRaster raster)
		{
			return null;
			// TODO Auto-generated method stub
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
		int startX = area == null ? 0 : area.x;
		int startY = area == null ? 0 : area.y;
		int endX = area == null ? w : startX + area.width;
		int endY = area == null ? h : startY + area.height;
		org.muis.core.style.Size radius = element.getStyle().get(org.muis.core.style.BackgroundStyles.cornerRadius);
		int wRad = radius.evaluate(w);
		int hRad = radius.evaluate(h);
		if(wRad * 2 > w)
			wRad = w / 2;
		if(hRad * 2 > h)
			hRad = h / 2;
		// The radius of the corner render we get needs to be either exactly the width or height radius or at least twice it for good
		// resolution.
		int maxRad = wRad;
		if(hRad != wRad)
		{
			if(hRad * 2 < wRad)
				maxRad = wRad;
			else if(hRad < wRad)
				maxRad = hRad * 2;
			else if(wRad * 2 < hRad)
				maxRad = hRad;
			else if(wRad < hRad)
				maxRad = wRad * 2;
		}
		float source = element.getStyle().get(org.muis.core.style.LightedStyle.lightSource).floatValue();
		CornerRenderKey key = new CornerRenderKey(source, maxRad);
		CornerRender cr = element.getDocument().getCache().getAndWait(element.getDocument(), cornerRendering, key, true);
		if(cr.getRadius() < maxRad)
		{
			// Regenerate with a big enough radius
			element.getDocument().getCache().remove(cornerRendering, key);
			cr = element.getDocument().getCache().getAndWait(element.getDocument(), cornerRendering, key, true);
		}

		// TODO

		float sin = (float) Math.sin(source * Math.PI / 180);
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
		}
		/*
		 * for(int y = startY; y < endY; y++) for(int x = startX; x < endX; x++) { float brighten = getBrighten(x, y, w, h, radius, sin,
		 * cos); if(brighten < 0) { graphics.setColor(new Color(0, 0, 0, (int) (-brighten * 255))); graphics.drawRect(x, y, 1, 1); } else
		 * if(brighten > 0) { graphics.setColor(new Color(255, 255, 255, (int) (brighten * 255))); graphics.drawRect(x, y, 1, 1); } }
		 */
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

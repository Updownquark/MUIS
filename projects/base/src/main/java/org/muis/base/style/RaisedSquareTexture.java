package org.muis.base.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.style.Texture;

/** Renders a raised, square, button-looking texture over an element */
public class RaisedSquareTexture implements Texture
{
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area)
	{
		int w = element.bounds().getWidth();
		int h = element.bounds().getHeight();
		org.muis.core.style.Size radius = element.getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.cornerRadius).get();
		int wRad = radius.evaluate(w);
		int hRad = radius.evaluate(h);
		java.awt.Color bg = org.muis.util.MuisUtils.getBackground(element.getStyle().getSelf()).get();
		if(bg.getAlpha() == 0)
			return;
		graphics.setColor(bg);
		graphics.fillRect(0, 0, w, h);
		float source = element.getStyle().getSelf().get(org.muis.core.style.LightedStyle.lightSource).get().floatValue();
		float maxShading = element.getStyle().getSelf().get(org.muis.core.style.LightedStyle.maxShadingAmount).get().floatValue();
		Color light = element.getStyle().getSelf().get(org.muis.core.style.LightedStyle.lightColor).get();
		Color shadow = element.getStyle().getSelf().get(org.muis.core.style.LightedStyle.shadowColor).get();
		int lightRGB = light.getRGB() & 0xffffff;
		int shadowRGB = shadow.getRGB() & 0xffffff;
		int sin = -(int) Math.round(255 * Math.sin(source * Math.PI / 180));
		int cos = (int) Math.round(255 * Math.sin(source * Math.PI / 180));

		// Top and bottom sides
		for(int y = 0; y < hRad; y++)
		{
			int startX = wRad * y / hRad + 1;
			int endX = w - startX - 1;
			int shading = (int) (maxShading * sin * (hRad - y) / hRad);
			Color lineColor;
			if(shading > 0)
				lineColor = new Color(lightRGB | (shading << 24), true);
			else if(shading < 0)
				lineColor = new Color(shadowRGB | ((-shading) << 24), true);
			else
				lineColor = null;

			if(lineColor != null)
			{
				graphics.setColor(lineColor);
				graphics.drawLine(startX, y, endX, y);
			}

			shading = (int) (maxShading * -sin * (hRad - y) / hRad);
			if(shading > 0)
				lineColor = new Color(lightRGB | (shading << 24), true);
			else if(shading < 0)
				lineColor = new Color(shadowRGB | ((-shading) << 24), true);
			else
				lineColor = null;

			if(lineColor != null)
			{
				graphics.setColor(lineColor);
				graphics.drawLine(startX, h - y - 1, endX, h - y - 1);
			}
		}

		// Left and right sides
		for(int x = 0; x < wRad; x++)
		{
			int startY = hRad * x / wRad + 1;
			int endY = h - startY - 1;
			int shading = (int) (maxShading * -cos * (wRad - x) / wRad);
			Color lineColor;
			if(shading > 0)
				lineColor = new Color(lightRGB | (shading << 24), true);
			else if(shading < 0)
				lineColor = new Color(shadowRGB | ((-shading) << 24), true);
			else
				lineColor = null;

			if(lineColor != null)
			{
				graphics.setColor(lineColor);
				graphics.drawLine(x, startY, x, endY);
			}

			shading = (int) (maxShading * cos * (wRad - x) / wRad);
			if(shading > 0)
				lineColor = new Color(lightRGB | (shading << 24), true);
			else if(shading < 0)
				lineColor = new Color(shadowRGB | ((-shading) << 24), true);
			else
				lineColor = null;

			if(lineColor != null)
			{
				graphics.setColor(lineColor);
				graphics.drawLine(w - x - 1, startY, w - x - 1, endY);
			}
		}
		// TODO Auto-generated method stub
	}
}

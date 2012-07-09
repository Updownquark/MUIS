package org.muis.base.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.style.Texture;

public class RaisedSquare implements Texture
{
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area)
	{
		Color bg = org.muis.core.MuisUtils.getBackground(element.getStyle());
		float source = element.getStyle().get(org.muis.core.style.LightedStyle.lightSource).floatValue();
		float maxShading = element.getStyle().get(org.muis.core.style.LightedStyle.maxShadingAmount).floatValue();
		Color light = element.getStyle().get(org.muis.core.style.LightedStyle.lightColor);
		Color shadow = element.getStyle().get(org.muis.core.style.LightedStyle.shadowColor);
		int bgRGB = bg.getRGB();
		int lightRGB = light.getRGB() & 0xffffff;
		int shadowRGB = shadow.getRGB() & 0xffffff;
		// TODO Auto-generated method stub
	}
}

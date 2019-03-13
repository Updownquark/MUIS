package org.quick.widget.base.style;

import java.awt.Color;
import java.awt.Graphics2D;

import org.quick.core.layout.Orientation;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.style.QuickWidgetTexture;

/** Implements a texture making an element look like a rounded groove */
public class GrooveTexture implements QuickWidgetTexture {
	@Override
	public void render(Graphics2D graphics, QuickWidget widget, Rectangle area) {
		int w = widget.bounds().getWidth();
		int h = widget.bounds().getHeight();
		Orientation orientation = widget.getElement().atts().get(org.quick.core.layout.LayoutAttributes.orientation).get();
		if(orientation == null)
			orientation = Orientation.horizontal;
		Color bg = org.quick.util.QuickUtils.getBackground(widget.getElement().getStyle()).get();
		if(bg.getAlpha() == 0)
			return;
		graphics.setColor(bg);
		graphics.fillRect(0, 0, w, h);
		float source = widget.getElement().getStyle().get(org.quick.core.style.LightedStyle.lightSource).get().floatValue();
		float maxShading = widget.getElement().getStyle().get(org.quick.core.style.LightedStyle.maxShadingAmount).get().floatValue();
		Color light = widget.getElement().getStyle().get(org.quick.core.style.LightedStyle.lightColor).get();
		Color shadow = widget.getElement().getStyle().get(org.quick.core.style.LightedStyle.shadowColor).get();

		int crossDir = widget.bounds().get(orientation.opposite()).getSize();
		short [] shading = getShadeAmount(source, maxShading, crossDir, orientation);
		for(int i = 0; i < crossDir; i++) {
			int lightRGB = (light.getRGB() & 0xffffff) | (shading[i] << 24);
			int shadowRGB = (shadow.getRGB() & 0xffffff) | ((255 - shading[i]) << 24);
			switch (orientation) {
			case horizontal:
				if(shading[i] > 0) {
					graphics.setColor(new Color(lightRGB));
					graphics.drawLine(0, i, w, i);
				}
				if(shading[i] < 255) {
					graphics.setColor(new Color(shadowRGB));
					graphics.drawLine(0, i, w, i);
				}
				break;
			case vertical:
				if(shading[i] > 0) {
					graphics.setColor(new Color(lightRGB));
					graphics.drawLine(i, 0, i, h);
				}
				if(shading[i] < 255) {
					graphics.setColor(new Color(shadowRGB));
					graphics.drawLine(i, 0, i, h);
				}
				break;
			}
		}
	}

	/**
	 * @param lightSource The light source angle, in degrees West of North
	 * @param maxShading The max shading amount, between 0 and 1
	 * @param width The width of the groove to shade
	 * @param orientation The orientation of the groove
	 * @return Shading amounts for each pixel across the groove
	 */
	public static short [] getShadeAmount(float lightSource, float maxShading, int width, Orientation orientation) {
		if(orientation != Orientation.vertical)
			lightSource -= 90;
		if(lightSource < 0)
			lightSource += 360;
		lightSource *= Math.PI / 180;
		float cos = (float) Math.cos(lightSource);
		short [] ret = new short[width];
		float halfW = (width - 1) / 2f;
		for(int x = 0; x <= width - 1; x++)
			ret[x] = (short) Math.round(255 * maxShading * (cos * (x - halfW) + halfW));
		return ret;
	}
}

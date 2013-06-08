package org.muis.base.style;

import java.awt.*;
import java.awt.geom.Arc2D;

import org.muis.core.MuisElement;
import org.muis.core.style.Texture;

/** Draws a border at the very edge of an element */
public class BorderTexture implements Texture {
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area) {
		int thickness = element.getStyle().getSelf().get(BorderStyle.thickness).intValue();
		if(thickness == 0)
			return;
		int w = element.bounds().getWidth();
		int h = element.bounds().getHeight();
		org.muis.core.style.Size radius = element.getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.cornerRadius);
		int wRad = radius.evaluate(w);
		int hRad = radius.evaluate(h);
		Color color = element.getStyle().getSelf().get(BorderStyle.color);
		float dashLength = element.getStyle().getSelf().get(BorderStyle.dashLength).floatValue();
		float dashInterim = element.getStyle().getSelf().get(BorderStyle.dashInterim).floatValue();
		BasicStroke stroke;
		if(dashInterim == 0)
			stroke = new BasicStroke(thickness);
		else
			stroke = new BasicStroke(thickness, 0, 0, 1, new float[] {dashLength, dashInterim}, 0);
		Stroke oldStroke = graphics.getStroke();
		Color oldColor = graphics.getColor();
		graphics.setStroke(stroke);
		graphics.setColor(color);

		if(wRad > 0 && hRad > 0) {
			// Corners
			// Upper left
			graphics.draw(new Arc2D.Double(thickness / 2, thickness / 2, wRad * 2, hRad * 2, Math.PI / 2, Math.PI / 2, Arc2D.OPEN));
			// Upper right
			graphics.draw(new Arc2D.Double(w - wRad * 2 - thickness / 2, thickness / 2, wRad * 2, hRad * 2, 0, Math.PI / 2, Arc2D.OPEN));
			// Lower right
			graphics.draw(new Arc2D.Double(w - wRad * 2 - thickness / 2, h - hRad * 2 - thickness / 2, wRad * 2, hRad * 2, -Math.PI / 2,
				Math.PI / 2, Arc2D.OPEN));
			// Lower left
			graphics.draw(new Arc2D.Double(thickness / 2, h - hRad * 2 - thickness / 2, wRad * 2, hRad * 2, Math.PI, Math.PI / 2,
				Arc2D.OPEN));

			// Sides
			// Top
			graphics.drawLine(wRad + thickness / 2, thickness / 2, w - wRad - thickness / 2, thickness / 2);
			// Right
			graphics.drawLine(w - thickness / 2, hRad + thickness / 2, w - thickness / 2, h - hRad - thickness / 2);
			// Bottom
			graphics.drawLine(wRad + thickness / 2, h - thickness / 2, w - wRad - thickness / 2, h - thickness / 2);
			// Left
			graphics.drawLine(thickness / 2, hRad + thickness / 2, thickness / 2, h - hRad - thickness / 2);
		} else
			graphics.drawRect(thickness / 2, thickness / 2, w - thickness, h - thickness);
		graphics.setColor(oldColor);
		graphics.setStroke(oldStroke);
	}
}

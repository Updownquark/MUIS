package org.quick.widget.base.style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Arc2D;

import org.quick.base.style.BorderStyle;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.style.BaseTexture;

/** Draws a border at the very edge of an element */
public class BorderTexture extends BaseTexture {
	@Override
	public void render(Graphics2D graphics, QuickWidget widget, Rectangle area) {
		super.render(graphics, widget, area);
		int thickness = widget.getElement().getStyle().get(BorderStyle.thickness).get().intValue();
		if(thickness == 0)
			return;
		int inset = widget.getElement().getStyle().get(BorderStyle.inset).get().intValue();
		int w = widget.bounds().getWidth();
		int h = widget.bounds().getHeight();
		org.quick.core.style.Size radius = widget.getElement().getStyle().get(org.quick.core.style.BackgroundStyle.cornerRadius).get();
		int wRad = radius.evaluate(w);
		int hRad = radius.evaluate(h);
		Color color = org.quick.util.QuickUtils.getColor(widget.getElement().getStyle().get(BorderStyle.color).get(),
			widget.getElement().getStyle().get(BorderStyle.transparency).get());
		float dashLength = widget.getElement().getStyle().get(BorderStyle.dashLength).get().floatValue();
		float dashInterim = widget.getElement().getStyle().get(BorderStyle.dashInterim).get().floatValue();
		BasicStroke stroke;
		if(dashInterim == 0)
			stroke = new BasicStroke(thickness);
		else
			stroke = new BasicStroke(thickness, 0, 0, 1, new float[] {dashLength, dashInterim}, 0);
		Stroke oldStroke = graphics.getStroke();
		Color oldColor = graphics.getColor();
		graphics.setStroke(stroke);
		graphics.setColor(color);
		Object oldRH = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if(wRad > 0 && hRad > 0) {
			double quarter = 90;
			int offset = inset + thickness / 2;
			// Corners
			// Upper left
			graphics.draw(new Arc2D.Double(offset, offset, wRad * 2, hRad * 2, quarter, quarter, Arc2D.OPEN));
			// Upper right
			graphics.draw(new Arc2D.Double(w - wRad * 2 - offset, offset, wRad * 2, hRad * 2, 0, quarter, Arc2D.OPEN));
			// Lower right
			graphics
				.draw(new Arc2D.Double(w - wRad * 2 - offset, h - hRad * 2 - offset, wRad * 2, hRad * 2, -quarter, quarter, Arc2D.OPEN));
			// Lower left
			graphics.draw(new Arc2D.Double(offset, h - hRad * 2 - offset, wRad * 2, hRad * 2, quarter * 2, quarter, Arc2D.OPEN));

			// Sides
			// Top
			graphics.drawLine(wRad + offset, offset, w - wRad - offset, offset);
			// Right
			graphics.drawLine(w - offset, hRad + offset, w - offset, h - hRad - offset);
			// Bottom
			graphics.drawLine(wRad + offset, h - offset, w - wRad - offset, h - offset);
			// Left
			graphics.drawLine(offset, hRad + offset, offset, h - hRad - offset);
		} else
			graphics.drawRect(inset + thickness / 2, inset + thickness / 2, w - thickness - inset * 2, h - thickness - inset * 2);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldRH);
		graphics.setColor(oldColor);
		graphics.setStroke(oldStroke);
	}
}

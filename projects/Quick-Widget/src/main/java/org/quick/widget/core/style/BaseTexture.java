package org.quick.widget.core.style;

import java.awt.Graphics2D;

import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.Size;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.Rectangle;

/** Renders the background style of the widget */
public class BaseTexture implements QuickWidgetTexture {
	@Override
	public void render(Graphics2D graphics, QuickWidget widget, Rectangle area) {
		int w = widget.bounds().getWidth();
		int h = widget.bounds().getHeight();
		int x = area == null ? 0 : area.x;
		int y = area == null ? 0 : area.y;
		int renderW = area == null ? widget.bounds().getWidth() : (area.width < w ? area.width : w);
		int renderH = area == null ? widget.bounds().getHeight() : (area.height < h ? area.height : h);
		java.awt.Color bg = org.quick.util.QuickUtils.getBackground(widget.getElement().getStyle()).get();
		if(bg.getAlpha() == 0)
			return;
		java.awt.Rectangle preClip = graphics.getClipBounds();
		graphics.setClip(x, y, renderW, renderH);
		graphics.setColor(bg);
		Size cornerRad = widget.getElement().getStyle().get(BackgroundStyle.cornerRadius).get();
		int wRad = cornerRad.evaluate(w);
		int hRad = cornerRad.evaluate(h);
		int wRad2 = wRad * 2;
		int hRad2 = hRad * 2;
		if(wRad2 > w) {
			wRad = w / 2;
			wRad2 = wRad * 2;
		}
		if(hRad2 > h) {
			hRad = h / 2;
			hRad2 = hRad * 2;
		}
		if(wRad >= hRad) {
			if(y >= hRad && y + renderH <= h - hRad)
				graphics.fillRect(x, y, w, h);
			else {
				graphics.fillRect(x, y + hRad, w, h - hRad2);
				// Left side
				if(x < wRad) {
					// Upper left corner
					if(y < hRad)
						graphics.fillArc(0, 0, wRad2, hRad2, 90, 90);
					// Lower left corner
					if(y + renderH > h - hRad)
						graphics.fillArc(0, h - hRad2, wRad2, hRad2, 180, 90);
				}
				// Right side
				if(x + renderW > w - wRad) {
					// Upper right corner
					if(y < hRad)
						graphics.fillArc(w - wRad2, 0, wRad2, hRad2, 0, 90);
					// Lower right corner
					if(y + renderH > h - hRad)
						graphics.fillArc(w - wRad2, h - hRad2, wRad2, hRad2, 270, 90);
				}
				graphics.fillRect(wRad, 0, w - wRad2, hRad);
				graphics.fillRect(wRad, h - hRad, w - wRad2, hRad);
			}
		} else {
			if(x >= wRad && x + renderW <= w - wRad)
				graphics.fillRect(x, y, w, h);
			else {
				graphics.fillRect(x + wRad, y, w - wRad2, h);
				// Top side
				if(y < hRad) {
					// Upper left corner
					if(x < wRad)
						graphics.fillArc(0, 0, wRad2, hRad2, 90, 90);
					// Upper right corner
					if(x + renderW > w - wRad)
						graphics.fillArc(w - wRad2, 0, wRad2, hRad2, 0, 90);
				}
				// Bottom side
				if(y + renderH > h - hRad) {
					// Lower left corner
					if(x < wRad)
						graphics.fillArc(0, h - hRad2, wRad2, hRad2, 180, 90);
					// Lower right corner
					if(x + renderW > w - wRad)
						graphics.fillArc(w - wRad2, h - hRad2, wRad2, hRad2, 270, 90);
				}
				graphics.fillRect(0, hRad, wRad, h - hRad2);
				graphics.fillRect(w - wRad, hRad, wRad, h - hRad2);
			}
		}
		graphics.setClip(preClip);
	}
}

package org.quick.widget.core.style;

import java.awt.Graphics2D;

import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.RenderableDocumentModel.StyledSequenceMetric;
import org.quick.widget.core.model.DocumentedElement;

/** Renders the background of a {@link DocumentedElement} in accordance with each text segment's style */
public class DocumentTexture extends BaseTexture {
	@Override
	public void render(Graphics2D graphics, QuickWidget widget, Rectangle area) {
		super.render(graphics, widget, area);
		DocumentedElement docEl = (DocumentedElement) widget.getElement();
		for (StyledSequenceMetric metric : docEl.getRenderableDocument().metrics(0, widget.bounds().getWidth())) {
			int x = (int) metric.getLeft();
			int y = (int) metric.getTop();
			int w = (int) metric.getWidth();
			int h = (int) metric.getHeight();
			Rectangle mBound = new Rectangle(x, y, w, h);
			if (area != null) {
				mBound = mBound.intersection(area);
				if (mBound == null || mBound.isEmpty())
					return;
			}
			java.awt.Color color = org.quick.util.QuickUtils.getBackground(metric.getStyle()).get();
			graphics.setColor(color);
			graphics.fillRect(mBound.x, mBound.y, mBound.width, mBound.height);
		}
	}
}

package org.quick.core.style;

import java.awt.Graphics2D;

import org.quick.core.QuickElement;
import org.quick.core.Rectangle;
import org.quick.core.model.DocumentedElement;
import org.quick.core.model.QuickDocumentModel;

/** Renders the background of a {@link DocumentedElement} in accordance with each text segment's style */
public class DocumentTexture extends BaseTexture {
	@Override
	public void render(Graphics2D graphics, QuickElement element, Rectangle area) {
		super.render(graphics, element, area);
		DocumentedElement docEl = (DocumentedElement) element;
		QuickDocumentModel doc = docEl.getDocumentModel().get();
		for(QuickDocumentModel.StyledSequenceMetric metric : doc.metrics(0, element.bounds().getWidth())) {
			int x = (int) metric.getLeft();
			int y = (int) metric.getTop();
			int w = (int) metric.getWidth();
			int h = (int) metric.getHeight();
			Rectangle mBound = new Rectangle(x, y, w, h);
			if (area != null) {
				mBound = mBound.intersection(area);
				if (mBound == null || mBound.width == 0 || mBound.height == 0)
					return;
			}
			java.awt.Color color = org.quick.util.QuickUtils.getBackground(metric.getStyle()).get();
			graphics.setColor(color);
			graphics.fillRect(mBound.x, mBound.y, mBound.width, mBound.height);
		}
	}
}

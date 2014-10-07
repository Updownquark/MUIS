package org.muis.core.style;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.model.DocumentedElement;
import org.muis.core.model.MuisDocumentModel;

/** Renders the background of a {@link DocumentedElement} in accordance with each text segment's style */
public class DocumentTexture extends BaseTexture {
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area) {
		super.render(graphics, element, area);
		DocumentedElement docEl = (DocumentedElement) element;
		MuisDocumentModel doc = docEl.getDocumentModel();
		for(MuisDocumentModel.StyledSequenceMetric metric : doc.metrics(0, element.bounds().getWidth())) {
			int x = (int) metric.getLeft();
			int y = (int) metric.getTop();
			int w = (int) metric.getWidth();
			int h = (int) metric.getHeight();
			Rectangle mBound = new Rectangle(x, y, w, h);
			if(area != null)
				mBound = mBound.intersection(area);
			java.awt.Color color = org.muis.util.MuisUtils.getBackground(metric.getStyle()).get();
			graphics.setColor(color);
			graphics.fillRect(mBound.x, mBound.y, mBound.width, mBound.height);
		}
	}
}

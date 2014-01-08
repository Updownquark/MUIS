package org.muis.core.style;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.model.DocumentedElement;
import org.muis.core.model.MuisDocumentModel;

/**
 * Renders the background of a {@link DocumentedElement} such that the background of selected text may be different than that behind
 * unselected text
 */
public class DocumentTexture extends BaseTexture {
	@Override
	public void render(Graphics2D graphics, MuisElement element, Rectangle area) {
		super.render(graphics, element, area);
		DocumentedElement docEl = (DocumentedElement) element;
		MuisDocumentModel doc = docEl.getDocumentModel();
		for(MuisDocumentModel.StyledSequenceMetric metric : doc.metrics(0, element.bounds().getWidth())) {
			java.awt.Color color = org.muis.util.MuisUtils.getBackground(metric.getStyle());
			int x = (int) metric.getLeft();
			int y = (int) metric.getTop();
			int w = (int) metric.getWidth();
			int h = (int) metric.getHeight();
			graphics.setColor(color);
			graphics.fillRect(x, y, w, h);
		}
	}
}

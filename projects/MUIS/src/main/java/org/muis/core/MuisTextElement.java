package org.muis.core;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.layout.AbstractSizeGuide;
import org.muis.core.layout.SimpleSizeGuide;
import org.muis.core.layout.SizeGuide;

/** A MUIS element that serves as a placeholder for text content which may be interspersed with element children in an element. */
public class MuisTextElement extends MuisLeaf {
	private org.muis.core.model.SimpleDocumentModel theDocument;

	/** Creates a MUIS text element */
	public MuisTextElement() {
		this("");
	}

	/**
	 * Creates a MUIS text element with text
	 *
	 * @param text The text for the element
	 */
	public MuisTextElement(String text) {
		getDefaultStyleListener().addDomain(org.muis.core.style.FontStyle.getDomainInstance());
		theDocument = new org.muis.core.model.SimpleDocumentModel(getStyle().getSelf());
		theDocument.append(text);
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				new org.muis.core.model.TextSelectionBehavior().install(MuisTextElement.this);
			}
		}, MuisConstants.CoreStage.PARSE_CHILDREN.toString(), 1);
	}

	/** @param text The text content for this element */
	public void setText(String text) {
		theDocument.setText(text);
		fireEvent(new org.muis.core.event.SizeNeedsChangedEvent(null), false, false);
	}

	/** @return This element's text content */
	public String getText() {
		return theDocument.toString();
	}

	/** @return This element's text document */
	public org.muis.core.model.SimpleDocumentModel getDocumentModel() {
		return theDocument;
	}

	@Override
	public SizeGuide getWSizer() {
		float maxW = 0;
		float lineW = 0;
		for(org.muis.core.model.MuisDocumentModel.StyledSequenceMetric metric : theDocument.metrics(0, Integer.MAX_VALUE)) {
			if(metric.isNewLine()) {
				if(lineW > maxW)
					maxW = lineW;
				lineW = 0;
			}
			lineW += metric.getWidth();
		}
		if(lineW > maxW)
			maxW = lineW;
		int max = Math.round(maxW);

		int min;
		if(getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap)) {
			maxW = 0;
			lineW = 0;
			for(org.muis.core.model.MuisDocumentModel.StyledSequenceMetric metric : theDocument.metrics(0, 1)) {
				if(metric.isNewLine()) {
					if(lineW > maxW)
						maxW = lineW;
					lineW = 0;
				}
				lineW += metric.getWidth();
			}
			if(lineW > maxW)
				maxW = lineW;
			min = Math.round(maxW);
		} else
			min = max;
		return new SimpleSizeGuide(min, min, max, max, max);
	}

	@Override
	public SizeGuide getHSizer() {
		return new AbstractSizeGuide() {
			private int theCachedWidth;

			private int theCachedHeight;

			private int theCachedBaseline;

			{
				theCachedWidth = -1;
			}

			private void getSizes(int crossSize) {
				if(crossSize == theCachedWidth)
					return;
				theCachedWidth = crossSize;
				float totalH = 0;
				float lineH = 0;
				float baselineOffset = -1;
				float baseline = -1;
				for(org.muis.core.model.MuisDocumentModel.StyledSequenceMetric metric : theDocument.metrics(0, crossSize)) {
					if(metric.isNewLine()) {
						totalH += lineH;
						if(baseline < 0 && baselineOffset >= 0)
							baseline = lineH - baselineOffset;
						lineH = 0;
					}
					float h = metric.getHeight();
					if(h > lineH)
						lineH = h;
					if(baselineOffset < 0)
						baseline = h - metric.getBaseline();
				}
				totalH += lineH;

				theCachedHeight = Math.round(totalH);
				theCachedBaseline = Math.round(baseline);
			}

			@Override
			public int getMinPreferred(int crossSize, boolean csMax) {
				getSizes(crossSize);
				return theCachedHeight;
			}

			@Override
			public int getMaxPreferred(int crossSize, boolean csMax) {
				getSizes(crossSize);
				return theCachedHeight;
			}

			@Override
			public int getMin(int crossSize, boolean csMax) {
				getSizes(crossSize);
				return theCachedHeight;
			}

			@Override
			public int getPreferred(int crossSize, boolean csMax) {
				getSizes(crossSize);
				return theCachedHeight;
			}

			@Override
			public int getMax(int crossSize, boolean csMax) {
				getSizes(crossSize);
				return theCachedHeight;
			}

			@Override
			public int getBaseline(int size) {
				if(theCachedWidth < 0)
					getSizes(Integer.MAX_VALUE);
				return theCachedBaseline;
			}
		};
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		theDocument.draw(graphics, area, bounds().getWidth());
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if(getTagName() != null)
			ret.append('<').append(getTagName()).append('>');
		else
			ret.append("<!TEXT>");
		ret.append(org.jdom2.output.Format.escapeText(new org.jdom2.output.EscapeStrategy() {
			@Override
			public boolean shouldEscape(char ch) {
				if(org.jdom2.Verifier.isHighSurrogate(ch)) {
					return true; // Safer this way per http://unicode.org/faq/utf_bom.html#utf8-4
				}
				return false;
			}
		}, "\n", theDocument.toString()));
		if(getTagName() != null)
			ret.append('<').append('/').append(getTagName()).append('>');
		else
			ret.append("</TEXT\u00a1>");
		return ret.toString();
	}
}

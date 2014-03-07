package org.muis.core;

import static org.muis.core.style.FontStyle.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.muis.core.layout.AbstractSizeGuide;
import org.muis.core.layout.SimpleSizeGuide;
import org.muis.core.layout.SizeGuide;
import org.muis.core.model.*;

/** A MUIS element that serves as a placeholder for text content which may be interspersed with element children in an element. */
public class MuisTextElement extends MuisLeaf implements org.muis.core.model.DocumentedElement {
	private static final class InternalWrappingDocumentModel extends WrappingDocumentModel {
		InternalWrappingDocumentModel(MuisDocumentModel model) {
			super(model);
		}

		@Override
		protected void setWrapped(MuisDocumentModel model) {
			super.setWrapped(model);
		}
	}

	private final InternalWrappingDocumentModel theDocument;

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
		this((MuisDocumentModel) null);
		setText(text);
	}

	/**
	 * Creates a MUIS text element with a document
	 *
	 * @param doc The document for this element
	 */
	public MuisTextElement(MuisDocumentModel doc) {
		if(doc == null)
			doc = new SimpleDocumentModel(getStyle().getSelf());
		setFocusable(true);
		getDefaultStyleListener().addDomain(org.muis.core.style.FontStyle.getDomainInstance());
		theDocument = new InternalWrappingDocumentModel(doc);
		theDocument.getDocumentModel().addContentListener(new MuisDocumentModel.ContentListener() {
			@Override
			public void contentChanged(MuisDocumentModel.ContentChangeEvent evt) {
				fireEvent(new org.muis.core.event.SizeNeedsChangedEvent(null), false, false);
				repaint(null, false);
			}
		});
		((SimpleDocumentModel) theDocument.getDocumentModel()).addSelectionListener(new SelectableDocumentModel.SelectionListener() {
			@Override
			public void selectionChanged(SelectableDocumentModel.SelectionChangeEvent evt) {
				if(isFontDifferentSelected())
					fireEvent(new org.muis.core.event.SizeNeedsChangedEvent(null), false, false);
				repaint(null, true);
			}

			private boolean isFontDifferentSelected() {
				return mayStyleDifferentSelected(size, family, slant, stretch, weight);
			}

			private boolean mayStyleDifferentSelected(org.muis.core.style.StyleAttribute<?>... atts) {
				if(!(theDocument.getWrapped() instanceof SimpleDocumentModel))
					return true;
				SimpleDocumentModel sdm = (SimpleDocumentModel) theDocument.getWrapped();
				for(org.muis.core.style.StyleAttribute<?> att : atts)
					if(!sdm.getNormalStyle().get(att).equals(sdm.getSelectedStyle().get(att)))
						return true;
				return false;
			}
		});
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				new org.muis.core.model.TextSelectionBehavior().install(MuisTextElement.this);
			}
		}, MuisConstants.CoreStage.PARSE_CHILDREN.toString(), 1);
	}

	/**
	 * @param text The text content for this element
	 * @throws UnsupportedOperationException If this element's document is not {@link MutableDocumentModel mutable}
	 */
	public void setText(String text) {
		MuisDocumentModel doc = theDocument.getDocumentModel();
		if(doc instanceof MutableDocumentModel)
			((MutableDocumentModel) doc).setText(text);
		else
			throw new UnsupportedOperationException("This text element's document is not mutable");
	}

	/** @return This element's text content */
	public String getText() {
		return theDocument.toString();
	}

	@Override
	public MuisDocumentModel getDocumentModel() {
		return theDocument.getDocumentModel();
	}

	/** @param docModel The new document model for this text element */
	public void setDocumentModel(MuisDocumentModel docModel) {
		if(docModel == null)
			docModel = new SimpleDocumentModel(getStyle().getSelf());
		theDocument.setWrapped(docModel);
	}

	/** @param listener The listener to listen for selection changes in this text element's document */
	public void addTextSelectionListener(SelectableDocumentModel.SelectionListener listener) {
		theDocument.addSelectionListener(listener);
	}

	/** @param listener The listener to stop listening for selection changes in this text element's document */
	public void removeTextSelectionListener(SelectableDocumentModel.SelectionListener listener) {
		theDocument.removeSelectionListener(listener);
	}

	@Override
	public SizeGuide getWSizer() {
		float maxW = 0;
		float lineW = 0;
		for(MuisDocumentModel.StyledSequenceMetric metric : theDocument.getDocumentModel().metrics(0, Integer.MAX_VALUE)) {
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
			for(MuisDocumentModel.StyledSequenceMetric metric : theDocument.getDocumentModel().metrics(0, 1)) {
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
				for(org.muis.core.model.MuisDocumentModel.StyledSequenceMetric metric : theDocument.getDocumentModel()
					.metrics(0, crossSize)) {
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
			public int getBaseline(int widgetSize) {
				if(theCachedWidth < 0)
					getSizes(Integer.MAX_VALUE);
				return theCachedBaseline;
			}
		};
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		theDocument.getDocumentModel().draw(graphics, area, bounds().getWidth());
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

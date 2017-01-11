package org.quick.core;

import static org.quick.core.style.FontStyle.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Iterator;

import org.observe.ObservableValue;
import org.observe.SimpleSettableValue;
import org.quick.core.layout.AbstractSizeGuide;
import org.quick.core.layout.SimpleSizeGuide;
import org.quick.core.layout.SizeGuide;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.QuickDocumentModel.StyleChangeEvent;
import org.quick.core.model.SimpleDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.style.FontStyle;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;

import com.google.common.reflect.TypeToken;

/** A Quick element that serves as a placeholder for text content which may be interspersed with element children in an element. */
@QuickElementType(attributes = { @AcceptAttribute(declaringClass = QuickTextElement.class, field = "multiLine") })
public class QuickTextElement extends QuickLeaf implements org.quick.core.model.DocumentedElement {
	/** Whether a text element's document supports multiple lines */
	public static final QuickAttribute<Boolean> multiLine = QuickAttribute.build("multi-line", QuickPropertyType.boole).build();

	private final SimpleSettableValue<QuickDocumentModel> theDocument;
	private final QuickDocumentModel theFlattenedDocument;
	private String theInitText;

	/** Creates a Quick text element */
	public QuickTextElement() {
		this("");
	}

	/**
	 * Creates a Quick text element with text
	 *
	 * @param text The text for the element
	 */
	public QuickTextElement(String text) {
		theInitText = text;
		setFocusable(true);
		getDefaultStyleListener().watch(FontStyle.getDomainInstance());
		theDocument = new SimpleSettableValue<>(TypeToken.of(QuickDocumentModel.class), false);
		theFlattenedDocument = QuickDocumentModel.flatten(theDocument);
		theFlattenedDocument.changes().act(evt -> {
			boolean needsResize = false;
			boolean needsRepaint = false;
			boolean repaintImmediate = false;
			if (evt instanceof ContentChangeEvent)
				needsResize = needsRepaint = true;
			else if (evt instanceof StyleChangeEvent) {
				needsResize = isFontDifferent((StyleChangeEvent) evt);
				needsRepaint = repaintImmediate = true;
			}
			if (needsResize)
				sizeNeedsChanged();
			if (needsRepaint)
				repaint(null, repaintImmediate);
		});
		life()//
			.runWhen(() -> {
				if (theDocument.get() == null)
					theDocument.set(getInitDocument(theInitText), null);
				theInitText = null;
			}, QuickConstants.CoreStage.INIT_SELF.toString(), 1)//
			.runWhen(() -> {
				new org.quick.core.model.TextSelectionBehavior().install(QuickTextElement.this);
			}, QuickConstants.CoreStage.PARSE_CHILDREN.toString(), 1)//
			.runWhen(() -> atts().getHolder(multiLine).act(evt -> sizeNeedsChanged()), QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/**
	 * Creates a Quick text element with a document
	 *
	 * @param doc The document for this element
	 */
	public QuickTextElement(QuickDocumentModel doc) {
		this("");
	}

	/**
	 * @param text The initial text for the document
	 * @return The initial document for the element after initialization. Will not be called if the
	 *         {@link #QuickTextElement(QuickDocumentModel)} constructor is used or if the document is otherwise initialized previously.
	 */
	protected QuickDocumentModel getInitDocument(String text) {
		return new SimpleDocumentModel(this, text);
	}

	private boolean isFontDifferent(QuickDocumentModel.StyleChangeEvent evt) {
		return mayStyleDiffer(evt, size, family, slant, stretch, weight);
	}

	private boolean mayStyleDiffer(StyleChangeEvent evt, org.quick.core.style.StyleAttribute<?>... atts) {
		if (evt.styleBefore() == null || evt.styleAfter() == null)
			return true; // Can't know for sure so gotta rerender
		Iterator<QuickDocumentModel.StyledSequence> oldStyles = evt.styleBefore().iterator();
		Iterator<QuickDocumentModel.StyledSequence> newStyles = evt.styleAfter().iterator();
		if (!oldStyles.hasNext() || newStyles.hasNext())
			return false;
		QuickDocumentModel.StyledSequence oldStyle = oldStyles.next();
		QuickDocumentModel.StyledSequence newStyle = newStyles.next();
		int oldPos = 0;
		int newPos = 0;
		int index;
		for (index = 0; index < evt.getStartIndex(); index++) {
			if (oldPos + oldStyle.length() <= index) {
				oldPos += oldStyle.length();
				if (oldStyles.hasNext())
					oldStyle = oldStyles.next();
				else {
					oldStyle = null;
					break;
				}
			}
			if (newPos + newStyle.length() <= index) {
				newPos += newStyle.length();
				if (newStyles.hasNext())
					newStyle = newStyles.next();
				else {
					newStyle = null;
					break;
				}
			}
		}
		if (oldStyle == null || newStyle == null)
			return false;
		for (; index < evt.getEndIndex(); index++) {
			if (oldPos + oldStyle.length() <= index) {
				oldPos += oldStyle.length();
				if (oldStyles.hasNext())
					oldStyle = oldStyles.next();
				else {
					oldStyle = null;
					break;
				}
			}
			if (newPos + newStyle.length() <= index) {
				newPos += newStyle.length();
				if (newStyles.hasNext())
					newStyle = newStyles.next();
				else {
					newStyle = null;
					break;
				}
			}
			for (org.quick.core.style.StyleAttribute<?> att : atts)
				if (!oldStyle.getStyle().get(att).equals(newStyle.getStyle().get(att)))
					return true;
		}
		return false;
	}

	/**
	 * @param text The text content for this element
	 * @throws UnsupportedOperationException If this element's document is not {@link MutableDocumentModel mutable}
	 */
	public void setText(String text) {
		QuickDocumentModel doc = theDocument.get();
		if(doc instanceof MutableDocumentModel)
			((MutableDocumentModel) doc).setText(text);
		else
			throw new UnsupportedOperationException("This text element's document is not mutable");
	}

	/** @return This element's text content */
	public String getText() {
		return getDocumentModel().get().toString();
	}

	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		return theDocument.unsettable();
	}

	/** @param docModel The new document model for this text element */
	public void setDocumentModel(QuickDocumentModel docModel) {
		if (docModel == null) {
			if (theDocument.get() == null)
				docModel = new SimpleDocumentModel(this);
			else
				return;
		}
		theDocument.set(docModel, null);
	}

	@Override
	public SizeGuide getWSizer() {
		float maxW = 0;
		float lineW = 0;
		for (QuickDocumentModel.StyledSequenceMetric metric : theDocument.get().metrics(0, Integer.MAX_VALUE)) {
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
		boolean isMultiLine = Boolean.TRUE.equals(atts().get(multiLine));
		maxW = 0;
		lineW = 0;
		for (QuickDocumentModel.StyledSequenceMetric metric : theDocument.get().metrics(0, 1)) {
			boolean newLine = metric.isNewLine();
			if (!newLine)
				newLine = isMultiLine && metric.charAt(metric.length() - 1) == '\n';
			if (metric.isNewLine()) {
				if (lineW > maxW)
					maxW = lineW;
				lineW = 0;
			}
			lineW += metric.getWidth();
		}
		if (lineW > maxW)
			maxW = lineW;
		min = Math.round(maxW);
		// Not quite sure why these padding values need to be here, but the text wraps unnecessarily if they're not
		min += Math.round(min / 50f);
		max += Math.round(max / 50f);
		return new SimpleSizeGuide(min, min, max, max, max);
	}

	@Override
	public SizeGuide getHSizer() {
		if (theDocument.get().length() == 0) {
			java.awt.Font font = org.quick.util.QuickUtils.getFont(getStyle()).get();
			java.awt.font.FontRenderContext context = new java.awt.font.FontRenderContext(font.getTransform(),
				getStyle().get(org.quick.core.style.FontStyle.antiAlias).get().booleanValue(), false);
			java.awt.font.LineMetrics metrics = font.getLineMetrics("Iq", context);
			int height = Math.round(metrics.getAscent() + metrics.getDescent());
			return new SimpleSizeGuide(height, height, height, height, height);
		}
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
				for (org.quick.core.model.QuickDocumentModel.StyledSequenceMetric metric : theDocument.get().metrics(0, crossSize)) {
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
		theDocument.get().draw(graphics, area, bounds().getWidth());
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if(getTagName() != null)
			ret.append('<').append(getTagName()).append('>');
		else
			ret.append("<!TEXT>");
		ret.append(org.jdom2.output.Format.escapeText(ch -> {
			if(org.jdom2.Verifier.isHighSurrogate(ch)) {
				return true; // Safer this way per http://unicode.org/faq/utf_bom.html#utf8-4
			}
		return false;
		}, "\n", theDocument.get().toString()));
		if(getTagName() != null)
			ret.append('<').append('/').append(getTagName()).append('>');
		else
			ret.append("</TEXT\u00a1>");
		return ret.toString();
	}
}

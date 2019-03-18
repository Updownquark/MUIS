package org.quick.widget.core;

import static org.quick.core.style.FontStyle.family;
import static org.quick.core.style.FontStyle.size;
import static org.quick.core.style.FontStyle.slant;
import static org.quick.core.style.FontStyle.stretch;
import static org.quick.core.style.FontStyle.weight;

import java.awt.Graphics2D;
import java.util.Iterator;

import org.observe.ObservableValue;
import org.quick.core.QuickConstants;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickException;
import org.quick.core.QuickTextElement;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.QuickDocumentModel.StyleChangeEvent;
import org.quick.core.style.FontStyle;
import org.quick.widget.core.RenderableDocumentModel.StyledSequenceMetric;
import org.quick.widget.core.layout.SimpleSizeGuide;
import org.quick.widget.core.layout.SizeGuide;
import org.quick.widget.core.model.DocumentedElement;

public class QuickTextWidget extends QuickWidget<QuickTextElement> implements DocumentedElement {
	private RenderableDocumentModel theDocumentModel;

	@Override
	public void init(QuickWidgetDocument document, QuickTextElement element, QuickDefinedWidget<QuickWidgetDocument, ?> parent)
		throws QuickException {
		super.init(document, element, parent);
		QuickDocumentModel theFlattenedDocument = QuickDocumentModel.flatten(getElement().getDocumentModel());
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
		theDocumentModel = new RenderableDocumentModel(theFlattenedDocument, element.getResourcePool());
		getDefaultStyleListener().watch(FontStyle.getDomainInstance());
		getElement().life().runWhen(() -> {
			new TextSelectionBehavior().install(QuickTextWidget.this);
		}, QuickConstants.CoreStage.PARSE_CHILDREN.toString(), 1);
		getElement().life().runWhen(() -> getElement().atts().get(QuickTextElement.multiLine).changes().act(evt -> sizeNeedsChanged()),
			QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		return getElement().getDocumentModel();
	}

	@Override
	public RenderableDocumentModel getRenderableDocument() {
		return theDocumentModel;
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		if (orientation.isVertical()) {
			if (getElement().getDocumentModel().get().length() == 0) {
				java.awt.Font font = org.quick.util.QuickUtils.getFont(getElement().getStyle()).get();
				java.awt.font.FontRenderContext context = new java.awt.font.FontRenderContext(font.getTransform(),
					getElement().getStyle().get(org.quick.core.style.FontStyle.antiAlias).get().booleanValue(), false);
				java.awt.font.LineMetrics metrics = font.getLineMetrics("Iq", context);
				int height = Math.round(metrics.getAscent() + metrics.getDescent());
				return new SimpleSizeGuide(height, height, height, height, height);
			}
			return new SizeGuide.GenericSizeGuide() {
				private int theCachedWidth;

				private int theCachedHeight;

				private int theCachedBaseline;

				{
					theCachedWidth = -1;
				}

				@Override
				public int get(LayoutGuideType type, int crossSize, boolean csMax) {
					if (crossSize == theCachedWidth)
						return theCachedHeight;
					theCachedWidth = crossSize;
					float totalH = 0;
					float lineH = 0;
					float baselineOffset = -1;
					float baseline = -1;
					for (StyledSequenceMetric metric : getRenderableDocument().metrics(0, crossSize)) {
						if (metric.isNewLine()) {
							totalH += lineH;
							if (baseline < 0 && baselineOffset >= 0)
								baseline = lineH - baselineOffset;
							lineH = 0;
						}
						float h = metric.getHeight();
						if (h > lineH)
							lineH = h;
						if (baselineOffset < 0)
							baseline = h - metric.getBaseline();
					}
					totalH += lineH;

					theCachedHeight = Math.round(totalH);
					theCachedBaseline = Math.round(baseline);
					return theCachedHeight;
				}

				@Override
				public int getBaseline(int widgetSize) {
					if (theCachedWidth < 0)
						get(LayoutGuideType.pref, Integer.MAX_VALUE, false);
					return theCachedBaseline;
				}
			};
		} else {
			float maxW = 0;
			float lineW = 0;
			for (StyledSequenceMetric metric : getRenderableDocument().metrics(0, Integer.MAX_VALUE)) {
				if (metric.isNewLine()) {
					if (lineW > maxW)
						maxW = lineW;
					lineW = 0;
				}
				lineW += metric.getWidth();
			}
			if (lineW > maxW)
				maxW = lineW;
			int max = Math.round(maxW);

			int min;
			boolean isMultiLine = Boolean.TRUE.equals(getElement().atts().get(QuickTextElement.multiLine).get());
			maxW = 0;
			lineW = 0;
			for (StyledSequenceMetric metric : getRenderableDocument().metrics(0, 1)) {
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
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		getRenderableDocument().draw(graphics, area, bounds().getWidth());
	}

	static boolean isFontDifferent(QuickDocumentModel.StyleChangeEvent evt) {
		return mayStyleDiffer(evt, size, family, slant, stretch, weight);
	}

	static boolean mayStyleDiffer(StyleChangeEvent evt, org.quick.core.style.StyleAttribute<?>... atts) {
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
}

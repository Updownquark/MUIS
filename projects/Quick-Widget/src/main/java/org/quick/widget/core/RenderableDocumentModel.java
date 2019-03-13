package org.quick.widget.core;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.qommons.DemandCache;
import org.qommons.Transaction;
import org.quick.core.mgr.ObservableResourcePool;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.StyledSequence;
import org.quick.core.style.FontStyle;
import org.quick.core.style.QuickStyle;
import org.quick.util.QuickUtils;

public class RenderableDocumentModel {
	/**
	 * A piece of text with style attributes and metrics information. The content of a StyledSequenceMetrics never contains a line break.
	 */
	public interface StyledSequenceMetric extends StyledSequence {
		/** @return The width of this piece of text */
		float getWidth();

		/** @return The height of this piece of text */
		float getHeight();

		/** @return The baseline of this piece of text */
		float getBaseline();

		/** @return Whether this sequence is the first of a new line. This may return false for the first line. */
		boolean isNewLine();

		@Override
		StyledSequenceMetric subSequence(int start, int end);

		/** @return The top position of this sequence in the document */
		float getTop();

		/** @return The left position of this sequence in the document */
		float getLeft();

		/**
		 * @param advance The x-coordinate, in pixels, relative to the left edge of this sequence
		 * @return The position, in characters, along this sequence that the given advance hits
		 */
		float getHitPosition(float advance);

		/**
		 * @param position The position, in characters, along this sequence
		 * @return The x-coordinate, in pixels, relative to the beginning of this sequence of the given position
		 */
		float getLocation(float position);

		/**
		 * Draws this sequence
		 *
		 * @param graphics The graphics to draw the text sequence to
		 * @param x The x-coordinate of the top left corner of the graphics to draw the text to
		 * @param y The y-coordinate of the top left corner of the graphics to draw the text to
		 */
		void draw(Graphics2D graphics, float x, float y);
	}

	private final QuickDocumentModel theModel;
	private DemandCache<Float, List<StyledSequenceMetric>> theMetricsCache;

	public RenderableDocumentModel(QuickDocumentModel model, ObservableResourcePool pool) {
		theModel = model;
		theMetricsCache = new org.qommons.DemandCache<>(true);
		theMetricsCache.setPreferredSize(5);
		theMetricsCache.setHalfLife(60000);
		pool.pool(theModel.changes()).act(v -> clearCache());
	}

	public QuickDocumentModel getDocumentModel() {
		return theModel;
	}

	/** Clears this document's internal metrics cache. This call is needed whenever the document's content or style changes. */
	protected void clearCache() {
		theMetricsCache.clear();
	}

	/**
	 * @param start The starting position within the document
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The metrics for this document. The first metric's {@link StyledSequenceMetric#isNewLine() newLine} attribute may be false
	 *         even if a new line occurred at position start-1.
	 */
	public Iterable<StyledSequenceMetric> metrics(final int start, final float breakWidth) {
		Iterable<StyledSequenceMetric> rawMetrics = () -> {
			return new MetricsIterator(getDocumentModel().iterateFrom(start).iterator(), breakWidth);
		};
		Iterable<StyledSequenceMetric> ret;
		if (start == 0)
			ret = theMetricsCache.computeIfAbsent(breakWidth, w -> //
			toList(rawMetrics));
		else
			ret = rawMetrics;
		return ret;
	}

	private static List<StyledSequenceMetric> toList(Iterable<StyledSequenceMetric> metrics) {
		ArrayList<StyledSequenceMetric> cached = new ArrayList<>();
		for (StyledSequenceMetric metric : metrics)
			cached.add(metric);
		cached.trimToSize();
		return java.util.Collections.unmodifiableList(cached);
	}

	/**
	 * @param x The x-coordinate, in pixels, relative to this document's top left corner
	 * @param y The y-coordinate, in pixels, relative to this document's top left corner
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The character position at the given point in the document
	 */
	public float getPositionAt(float x, float y, int breakWidth) {
		try (Transaction t = getDocumentModel().holdForRead()) {
			int pos = 0;
			float totalH = 0;
			float lineW = 0, lineH = 0;
			int linePos = 0;
			if (y > 0) {
				for (StyledSequenceMetric metric : metrics(0, breakWidth)) {
					if (metric.isNewLine()) {
						totalH += lineH;
						lineH = 0;
						lineW = 0;
						linePos = pos;
					}
					lineW += metric.getWidth();
					float h = metric.getHeight();
					if (h > lineH)
						lineH = h;
					if (totalH + lineH > y)
						break;
					pos += metric.length();
				}
				lineW = 0;
				pos = linePos;
			}
			if (x <= 0)
				return pos;
			boolean firstMetric = true;
			for (StyledSequenceMetric metric : metrics(linePos, breakWidth)) {
				if (!firstMetric && metric.isNewLine())
					return pos;
				firstMetric = false;
				if (lineW + metric.getWidth() > x) {
					return pos + metric.getHitPosition(x - lineW);
				}
				lineW += metric.getWidth();
				pos += metric.length();
			}
			return pos;
		}
	}

	/**
	 * @param position The character position in the document
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The position in the document at the given character at the top of the line
	 */
	public Point2D getLocationAt(float position, int breakWidth) {
		if (position < 0)
			throw new IndexOutOfBoundsException("" + position);
		if (position == 0)
			return new Point(0, 0);
		try (Transaction t = getDocumentModel().holdForRead()) {
			if (position > getDocumentModel().length())
				throw new IndexOutOfBoundsException(position + ">" + getDocumentModel().length());
			float totalH = 0;
			float lineH = 0;
			float lineW = 0;
			int pos = 0;
			int linePos = 0;
			for (StyledSequenceMetric metric : metrics(0, breakWidth)) {
				if (metric.isNewLine()) {
					if (pos >= position)
						break;
					totalH += lineH;
					lineH = 0;
					lineW = 0;
					linePos = pos;
				}
				lineW += metric.getWidth();
				float h = metric.getHeight();
				if (h > lineH)
					lineH = h;
				pos += metric.length();
			}
			if (position == getDocumentModel().length())
				return new Point2D.Float(lineW, totalH);
			lineW = 0;
			pos = linePos;
			for (StyledSequenceMetric metric : metrics(linePos, breakWidth)) {
				if (pos + metric.length() > position) {
					return new Point2D.Float(lineW + metric.getLocation(position - pos), totalH);
				}
				lineW += metric.getWidth();
				pos += metric.length();
			}
		}
		throw new IllegalStateException("Metrics calculation failed");
	}

	/**
	 * Draws a portion of this document
	 *
	 * @param graphics The graphics to draw on
	 * @param window The window within this document to draw
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 */
	public void draw(Graphics2D graphics, Rectangle window, int breakWidth) {
		float totalH = 0;
		float lineH = 0;
		org.qommons.FloatList lineHeights = new org.qommons.FloatList();
		try (Transaction t = getDocumentModel().holdForRead()) {
			int linePos = 0;
			int pos = 0;
			int startLine = -1;
			int startLinePos = -1;
			for (StyledSequenceMetric metric : metrics(0, breakWidth)) {
				if (metric.isNewLine()) {
					lineHeights.add(lineH);
					totalH += lineH;
					if (window != null && startLine < 0 && totalH > window.y) {
						startLine = lineHeights.size() - 1;
						startLinePos = linePos;
					}
					if (window != null && totalH > window.getMaxY())
						break;
					lineH = 0;
					linePos = pos;
				}
				float h = metric.getHeight();
				if (h > lineH)
					lineH = h;
				pos += metric.length();
			}
			lineHeights.add(lineH);
			totalH += lineH;
			if (window != null && startLine < 0 && totalH > window.y) {
				startLine = lineHeights.size() - 1;
				startLinePos = linePos;
			}
			if (window != null && startLine < 0)
				return; // No content to draw within window
			else {
				startLine = 0;
				startLinePos = 0;
			}

			totalH = 0;
			lineH = 0;
			float lineW = 0;
			float startHeight = 0;
			for (int i = 0; i < startLine - 1; i++)
				startHeight += lineHeights.get(i);
			int lineNumber = startLine;
			totalH = startHeight;
			lineH = lineHeights.get(startLine);
			Rectangle oldClip = Rectangle.fromAwt(graphics.getClipBounds());
			if (window != null)
				graphics.setClip(window.x, window.y, window.width, window.height);
			try {
				boolean firstMetric = true;
				for (StyledSequenceMetric metric : metrics(startLinePos, breakWidth)) {
					if (!firstMetric && metric.isNewLine()) {
						totalH += lineH;
						if (window != null && totalH > window.getMaxY())
							break;
						lineNumber++;
						lineH = lineHeights.get(lineNumber);
						lineW = 0;
					}
					firstMetric = false;
					if (window == null || (lineW < window.getMaxX() && lineW + metric.getWidth() > window.x
						&& totalH + lineH - metric.getHeight() < window.getMaxY())) {
						graphics.setFont(QuickUtils.getFont(metric.getStyle()).get());
						metric.draw(graphics, lineW, totalH + lineH - metric.getHeight());
					}
					lineW += metric.getWidth();
				}
			} finally {
				if (window != null)
					graphics.setClip(oldClip.toAwt());
			}
		}
	}

	/**
	 * Implements {@link QuickDocumentModel#metrics(int, float)}
	 */
	private static class MetricsIterator implements Iterator<StyledSequenceMetric> {
		private final Iterator<StyledSequence> theBackingIterator;

		private final float theBreakWidth;

		private StyledSequence theCurrentSequence;
		private java.awt.font.LineBreakMeasurer theCurrentMeasurer;

		private Font theFont;

		private java.awt.font.FontRenderContext theContext;
		private TextLayout theCurrentLayout;
		private TextLayout theOldLayout;

		private float theLineOffset;
		private float theTop;
		private int theSequenceOffset;
		private boolean oldSequenceWasLineBreak;
		private boolean oldSequenceWasWordWrap;
		private boolean wasLineBreak;
		private boolean newMeasurer;

		MetricsIterator(Iterator<StyledSequence> backing, float breakWidth) {
			theBackingIterator = backing;
			theBreakWidth = breakWidth;
		}

		@Override
		public boolean hasNext() {
			while (theCurrentLayout == null) {
				if (theCurrentMeasurer != null && theCurrentSequence != null
					&& theCurrentMeasurer.getPosition() == theCurrentSequence.length()) {
					oldSequenceWasLineBreak = theCurrentSequence.charAt(theCurrentSequence.length() - 1) == '\n';
					oldSequenceWasWordWrap = theCurrentSequence.getStyle().get(FontStyle.wordWrap).get();
					theCurrentSequence = null;
					theCurrentMeasurer = null;
					theSequenceOffset = 0;
				}
				if (theCurrentSequence == null) {
					do {
						theCurrentSequence = null;
						if (theBackingIterator.hasNext())
							theCurrentSequence = theBackingIterator.next();
					} while (theCurrentSequence != null && theCurrentSequence.length() == 0);
					if (theCurrentSequence == null)
						return false;
					setMeasurer(theCurrentSequence);
					newMeasurer = true;
				}
				// Determine whether the next sequence should be on a new line
				if (newMeasurer) {
					wasLineBreak = oldSequenceWasLineBreak;
					newMeasurer = false;
				} else {
					wasLineBreak = oldSequenceWasWordWrap;
				}

				if (wasLineBreak) {
					if (theOldLayout != null)
						theTop += theOldLayout.getAscent() + theOldLayout.getDescent() + theOldLayout.getLeading();
					theLineOffset = 0;
				} else if (theOldLayout != null) {
					theLineOffset += theOldLayout.getAdvance();
				}
				theOldLayout = null;

				float width = theBreakWidth - theLineOffset;
				if (width <= 0)
					width = 1;
				if (theCurrentMeasurer == null)
					theCurrentLayout = null;
				else
					theCurrentLayout = theCurrentMeasurer.nextLayout(width);
			}
			return theCurrentLayout != null;
		}

		@Override
		public StyledSequenceMetric next() {
			if (!hasNext())
				throw new java.util.NoSuchElementException();
			TextLayout layout = theCurrentLayout;
			theCurrentLayout = null;
			theOldLayout = layout;
			int seqOffset = theSequenceOffset;
			theSequenceOffset += layout.getCharacterCount();
			return new StyledSequenceMetricsImpl(theCurrentSequence, layout, theFont, theContext, theTop, theLineOffset, seqOffset,
				wasLineBreak);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void setMeasurer(StyledSequence seq) {
			theFont = org.quick.util.QuickUtils.getFont(seq.getStyle()).get();
			theContext = new java.awt.font.FontRenderContext(theFont.getTransform(),
				seq.getStyle().get(org.quick.core.style.FontStyle.antiAlias).get().booleanValue(), false);
			java.text.AttributedString attrStr = new java.text.AttributedString(seq.toString());
			attrStr.addAttributes(theFont.getAttributes(), 0, seq.length());
			theCurrentMeasurer = new java.awt.font.LineBreakMeasurer(attrStr.getIterator(), java.text.BreakIterator.getWordInstance(),
				theContext);
		}

		private static class StyledSequenceMetricsImpl implements StyledSequenceMetric {
			private final StyledSequence theSequence;
			private final TextLayout theLayout;

			private final Font theFont;

			private final java.awt.font.FontRenderContext theContext;
			private final float theTop;
			private final float theLeft;
			private final int theOffset;
			private final boolean isNewLine;

			StyledSequenceMetricsImpl(StyledSequence sequence, TextLayout layout, Font font, java.awt.font.FontRenderContext ctx, float top,
				float left, int offset, boolean newLine) {
				theSequence = sequence;
				theLayout = layout;
				theFont = font;
				theContext = ctx;
				theTop = top;
				theLeft = left;
				theOffset = offset;
				isNewLine = newLine;
			}

			@Override
			public QuickStyle getStyle() {
				return theSequence.getStyle();
			}

			@Override
			public int length() {
				return theLayout.getCharacterCount();
			}

			@Override
			public char charAt(int index) {
				return theSequence.charAt(theOffset + index);
			}

			@Override
			public StyledSequenceMetric subSequence(int start, int end) {
				if (start > theOffset + theLayout.getCharacterCount())
					throw new IndexOutOfBoundsException(start + ">" + (theOffset + theLayout.getCharacterCount()));
				if (end > theOffset + theLayout.getCharacterCount())
					throw new IndexOutOfBoundsException(end + ">" + (theOffset + theLayout.getCharacterCount()));
				String content = theSequence.subSequence(theOffset + start, theOffset + end).toString();
				TextLayout layout = new TextLayout(content, theFont, theContext);
				return new StyledSequenceMetricsImpl(theSequence, layout, theFont, theContext, theTop, theLeft + getLocation(start),
					theOffset + start, isNewLine && start == 0);
			}

			@Override
			public float getTop() {
				return theTop;
			}

			@Override
			public float getLeft() {
				return theLeft;
			}

			@Override
			public float getWidth() {
				return theLayout.getAdvance();
			}

			@Override
			public float getHeight() {
				return theLayout.getAscent() + theLayout.getDescent();
			}

			@Override
			public float getBaseline() {
				return theLayout.getBaseline();
			}

			@Override
			public boolean isNewLine() {
				return isNewLine;
			}

			@Override
			public float getHitPosition(float advance) {
				int left = theLayout.hitTestChar(advance, 0).getCharIndex();
				if (left == theSequence.length())
					return left;
				float leftW = left == 0 ? 0 : subSequence(0, left).getWidth();
				float rightW = subSequence(0, left + 1).getWidth();
				return left + ((advance - leftW) / (rightW - leftW));
			}

			@Override
			public float getLocation(float position) {
				int pos = (int) position;
				float left = pos == 0 ? 0 : subSequence(0, pos).getWidth();
				if (position - (int) position == 0) {
					return left;
				}
				float right = subSequence(0, (int) position + 1).getWidth();
				return left + (position - (int) position) * (right - left);
			}

			@Override
			public void draw(Graphics2D graphics, float x, float y) {
				theLayout.draw(graphics, x, y + theLayout.getAscent());
			}

			@Override
			public String toString() {
				String str = theSequence.toString();
				if (theOffset >= str.length())
					return "";
				int end = theOffset + theLayout.getCharacterCount();
				if (end <= str.length())
					return str.substring(theOffset, end);
				else
					return str.substring(theOffset);
			}
		}
	}
}

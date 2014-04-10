package org.muis.core.model;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.muis.core.style.MuisStyle;

/**
 * Provides most of the implementation needed for a {@link MuisDocumentModel}, requiring the concrete subclass only to define
 * {@link #iterator()}
 */
public abstract class AbstractMuisDocumentModel implements MuisDocumentModel {
	private prisms.util.DemandCache<Float, Iterable<StyledSequenceMetric>> theMetricsCache;

	/** Creates the document */
	public AbstractMuisDocumentModel() {
		theMetricsCache = new prisms.util.DemandCache<>();
		theMetricsCache.setPreferredSize(5);
		theMetricsCache.setHalfLife(60000);
	}

	/** Clears this document's internal metrics cache. This call is needed whenever the document's content or style changes. */
	protected void clearCache() {
		theMetricsCache.clear();
	}

	@Override
	public int length() {
		int ret = 0;
		for(StyledSequence seq : this)
			ret += seq.length();
		return ret;
	}

	@Override
	public char charAt(int index) {
		int pos = 0;
		for(StyledSequence seq : this) {
			int seqLen = seq.length();
			if(pos + seqLen > index)
				return seq.charAt(index - pos);
			pos += seqLen;
		}
		throw new IndexOutOfBoundsException(index + ">" + pos);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if(start > end)
			throw new IndexOutOfBoundsException(start + ">" + end);
		StringBuilder ret = null;
		int pos = 0;
		for(StyledSequence seq : this) {
			int seqLen = seq.length();
			if(ret == null) {
				if(pos + seqLen >= start) {
					ret = new StringBuilder();
				}
			}
			if(ret != null) {
				if(start > pos) {
					if(end > 0 && pos + seqLen > end)
						ret.append(seq.subSequence(start - pos, end - pos));
					else
						ret.append(seq.subSequence(start - pos, seqLen));
				} else {
					if(end > 0 && pos + seqLen > end)
						ret.append(seq.subSequence(0, end - pos));
					else
						ret.append(seq.toString());
				}
			}
			pos += seqLen;
			if(end <= pos)
				break;
		}
		if(ret == null)
			throw new IndexOutOfBoundsException(start + ">" + pos);
		else if(end > 0)
			throw new IndexOutOfBoundsException(end + ">" + pos);
		else
			return ret.toString();
	}

	@Override
	public Iterable<StyledSequence> iterateFrom(final int position) {
		final Iterator<StyledSequence> iterator = iterator();
		int pos = 0;
		while(iterator.hasNext()) {
			final StyledSequence seq = iterator.next();
			int seqLen = seq.length();
			if(pos + seqLen > position) {
				final int fPos = pos;
				return new Iterable<StyledSequence>() {
					@Override
					public Iterator<StyledSequence> iterator() {
						return new Iterator<StyledSequence>() {
							private final StyledSequence theBegin = new StyledSequence() {
								private StyledSequence theBackingSequence = seq;

								private int theOffset = position - fPos;

								@Override
								public int length() {
									return theBackingSequence.length() - theOffset;
								}

								@Override
								public char charAt(int index) {
									return theBackingSequence.charAt(index + theOffset);
								}

								@Override
								public CharSequence subSequence(int start, int end) {
									return theBackingSequence.subSequence(start + theOffset, end + theOffset);
								}

								@Override
								public MuisStyle getStyle() {
									return theBackingSequence.getStyle();
								}

								@Override
								public String toString() {
									if(theOffset == 0)
										return theBackingSequence.toString();
									else
										return theBackingSequence.subSequence(theOffset, theBackingSequence.length()).toString();
								}
							};

							private boolean hasReturnedBegin;

							private Iterator<StyledSequence> theBackingIterator = iterator;

							@Override
							public boolean hasNext() {
								if(!hasReturnedBegin)
									return true;
								else
									return theBackingIterator.hasNext();
							}

							@Override
							public StyledSequence next() {
								if(!hasReturnedBegin) {
									hasReturnedBegin = true;
									return theBegin;
								} else
									return theBackingIterator.next();
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
					}
				};
			}
			pos += seqLen;
		}
		if(pos == position)
			return java.util.Collections.EMPTY_SET;
		else
			throw new IndexOutOfBoundsException(position + ">" + pos);
	}

	@Override
	public MuisStyle getStyleAt(int position) {
		int pos = 0;
		MuisStyle last = null;
		for(StyledSequence seq : this) {
			last = seq.getStyle();
			pos += seq.length();
			if(pos > position)
				break;
		}
		if(position > pos)
			throw new IndexOutOfBoundsException(position + ">" + pos);
		return last;
	}

	@Override
	public Iterable<StyledSequenceMetric> metrics(final int start, final float breakWidth) {
		Iterable<StyledSequenceMetric> ret;
		if(start == 0) {
			ret = theMetricsCache.get(breakWidth);
			if(ret == null) {
				ret = prisms.util.ArrayUtils.cachingIterable(new MetricsIterator(iterateFrom(start).iterator(), breakWidth));
				theMetricsCache.put(breakWidth, ret);
			}
			return ret;
		} else {
			return new Iterable<StyledSequenceMetric>() {
				@Override
				public Iterator<StyledSequenceMetric> iterator() {
					return new MetricsIterator(iterateFrom(start).iterator(), breakWidth);
				}
			};
		}
	}

	@Override
	public float getPositionAt(float x, float y, int breakWidth) {
		int pos = 0;
		float totalH = 0;
		float lineW = 0, lineH = 0;
		int linePos = 0;
		if(y > 0) {
			for(StyledSequenceMetric metric : metrics(0, breakWidth)) {
				if(metric.isNewLine()) {
					totalH += lineH;
					lineH = 0;
					lineW = 0;
					linePos = pos;
				}
				lineW += metric.getWidth();
				float h = metric.getHeight();
				if(h > lineH)
					lineH = h;
				if(totalH + lineH > y)
					break;
				pos += metric.length();
			}
			lineW = 0;
			pos = linePos;
		}
		if(x <= 0)
			return pos;
		boolean firstMetric = true;
		for(StyledSequenceMetric metric : metrics(linePos, breakWidth)) {
			if(!firstMetric && metric.isNewLine())
				return pos;
			firstMetric = false;
			if(lineW + metric.getWidth() > x) {
				return pos + metric.getHitPosition(x - lineW);
			}
			lineW += metric.getWidth();
			pos += metric.length();
		}
		return pos;
	}

	@Override
	public Point2D getLocationAt(float position, int breakWidth) {
		if(position < 0)
			throw new IndexOutOfBoundsException("" + position);
		if(position == 0)
			return new Point(0, 0);
		else if(position > length())
			throw new IndexOutOfBoundsException(position + ">" + length());
		float totalH = 0;
		float lineH = 0;
		float lineW = 0;
		int pos = 0;
		int linePos = 0;
		for(StyledSequenceMetric metric : metrics(0, breakWidth)) {
			if(metric.isNewLine()) {
				if(pos >= position)
					break;
				totalH += lineH;
				lineH = 0;
				lineW = 0;
				linePos = pos;
			}
			lineW += metric.getWidth();
			float h = metric.getHeight();
			if(h > lineH)
				lineH = h;
			pos += metric.length();
		}
		if(position == length())
			return new Point2D.Float(lineW, totalH);
		lineW = 0;
		pos = linePos;
		for(StyledSequenceMetric metric : metrics(linePos, breakWidth)) {
			if(pos + metric.length() > position) {
				return new Point2D.Float(lineW + metric.getLocation(position - pos), totalH);
			}
			lineW += metric.getWidth();
			pos += metric.length();
		}
		throw new IllegalStateException("Metrics calculation failed");
	}

	@Override
	public void draw(Graphics2D graphics, Rectangle window, int breakWidth) {
		float totalH = 0;
		float lineH = 0;
		prisms.util.FloatList lineHeights = new prisms.util.FloatList();
		int linePos = 0;
		int pos = 0;
		int startLine = -1;
		int startLinePos = -1;
		for(StyledSequenceMetric metric : metrics(0, breakWidth)) {
			if(metric.isNewLine()) {
				lineHeights.add(lineH);
				totalH += lineH;
				if(window != null && startLine < 0 && totalH > window.getMinY()) {
					startLine = lineHeights.size() - 1;
					startLinePos = linePos;
				}
				if(window != null && totalH > window.getMaxY())
					break;
				lineH = 0;
				linePos = pos;
			}
			float h = metric.getHeight();
			if(h > lineH)
				lineH = h;
			pos += metric.length();
		}
		lineHeights.add(lineH);
		totalH += lineH;
		if(window != null && startLine < 0 && totalH > window.getMinY()) {
			startLine = lineHeights.size() - 1;
			startLinePos = linePos;
		}
		if(window != null && startLine < 0)
			return; // No content to draw within window
		else {
			startLine = 0;
			startLinePos = 0;
		}

		totalH = 0;
		lineH = 0;
		float lineW = 0;
		float startHeight = 0;
		for(int i = 0; i < startLine - 1; i++)
			startHeight += lineHeights.get(i);
		int lineNumber = startLine;
		totalH = startHeight;
		lineH = lineHeights.get(startLine);
		Rectangle oldClip = graphics.getClipBounds();
		if(window != null)
			graphics.setClip(window.x, window.y, window.width, window.height);
		try {
			boolean firstMetric = true;
			for(StyledSequenceMetric metric : metrics(startLinePos, breakWidth)) {
				if(!firstMetric && metric.isNewLine()) {
					totalH += lineH;
					if(window != null && totalH > window.getMaxY())
						break;
					lineNumber++;
					lineH = lineHeights.get(lineNumber);
					lineW = 0;
				}
				firstMetric = false;
				if(window == null
					|| (lineW < window.getMaxX() && lineW + metric.getWidth() > window.getMinX() && totalH + lineH - metric.getHeight() < window
						.getMaxY()))
					metric.draw(graphics, lineW, totalH + lineH - metric.getHeight());
				lineW += metric.getWidth();
			}
		} finally {
			if(window != null)
				graphics.setClip(oldClip);
		}
	}

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
		private boolean wasLineBreak;
		private boolean newMeasurer;

		MetricsIterator(Iterator<StyledSequence> backing, float breakWidth) {
			theBackingIterator = backing;
			theBreakWidth = breakWidth;
		}

		@Override
		public boolean hasNext() {
			while(theCurrentLayout == null) {
				if(theCurrentMeasurer != null && theCurrentSequence != null
					&& theCurrentMeasurer.getPosition() == theCurrentSequence.length()) {
					oldSequenceWasLineBreak = theCurrentSequence.charAt(theCurrentSequence.length() - 1) == '\n';
					theCurrentSequence = null;
					theCurrentMeasurer = null;
					theSequenceOffset = 0;
				}
				if(theCurrentSequence == null) {
					if(theBackingIterator.hasNext()) {
						theCurrentSequence = theBackingIterator.next();
						setMeasurer(theCurrentSequence);
						newMeasurer = true;
					} else
						return false;
				}
				// Determine whether the next sequence should be on a new line
				if(newMeasurer) {
					wasLineBreak = oldSequenceWasLineBreak;
					newMeasurer = false;
				} else {
					wasLineBreak = true;
				}

				if(wasLineBreak) {
					if(theOldLayout != null)
						theTop += theOldLayout.getAscent() + theOldLayout.getDescent() + theOldLayout.getLeading();
					theLineOffset = 0;
				} else if(theOldLayout != null) {
					theLineOffset += theOldLayout.getAdvance();
				}
				theOldLayout = null;

				float width = theBreakWidth - theLineOffset;
				if(width <= 0)
					width = 1;
				theCurrentLayout = theCurrentMeasurer.nextLayout(width);
			}
			return theCurrentLayout != null;
		}

		@Override
		public StyledSequenceMetric next() {
			if(!hasNext())
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
			theFont = org.muis.util.MuisUtils.getFont(seq.getStyle());
			theContext = new java.awt.font.FontRenderContext(theFont.getTransform(), seq.getStyle()
				.get(org.muis.core.style.FontStyle.antiAlias).booleanValue(), false);
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

			StyledSequenceMetricsImpl(StyledSequence sequence, TextLayout layout, Font font, java.awt.font.FontRenderContext ctx,
				float top, float left, int offset, boolean newLine) {
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
			public MuisStyle getStyle() {
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
				if(start > theOffset + theLayout.getCharacterCount())
					throw new IndexOutOfBoundsException(start + ">" + (theOffset + theLayout.getCharacterCount()));
				if(end > theOffset + theLayout.getCharacterCount())
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
				if(left == theSequence.length())
					return left;
				float leftW = left == 0 ? 0 : subSequence(0, left).getWidth();
				float rightW = subSequence(0, left + 1).getWidth();
				return left + ((advance - leftW) / (rightW - leftW));
			}

			@Override
			public float getLocation(float position) {
				int pos=(int) position;
				float left = pos==0 ? 0 : subSequence(0, pos).getWidth();
				if(position - (int) position == 0) {
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
				return theSequence.subSequence(theOffset, theOffset + theLayout.getCharacterCount()).toString();
			}
		}
	}
}

package org.muis.core;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import org.muis.core.layout.SimpleSizePolicy;
import org.muis.core.layout.SizePolicy;

/** A MUIS element that serves as a placeholder for text content which may be interspersed with element children in an element. */
public class MuisTextElement extends MuisLeaf {
	private String theText;

	/** Creates a MUIS text element */
	public MuisTextElement() {
		getDefaultStyleListener().addDomain(org.muis.core.style.FontStyle.getDomainInstance());
	}

	/**
	 * Creates a MUIS text element with text
	 *
	 * @param text The text for the element
	 */
	public MuisTextElement(String text) {
		theText = text;
	}

	/** @param text The text content for this element */
	public void setText(String text) {
		theText = text;
	}

	/** @return This element's text content */
	public String getText() {
		return theText;
	}

	@Override
	public SizePolicy getWSizer(int height) {
		if(theText.length() == 0)
			return new SimpleSizePolicy(0, 0, 0, 0);
		java.awt.Font font = MuisUtils.getFont(getStyle().getSelf());
		if(font == null) {
			msg().error("Could not derive font");
			return new SimpleSizePolicy(0, 0, 0, 0);
		}
		java.awt.font.FontRenderContext context = new java.awt.font.FontRenderContext(null, getStyle().getSelf()
			.get(org.muis.core.style.FontStyle.antiAlias).booleanValue(), false);
		int lineIdx = 0;
		if(getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap)) {
			int min = 0;
			int max = 0;
			int wordIdx = 0;
			for(int c = 0; c < theText.length(); c++) {
				char ch = theText.charAt(c);
				if(ch == ' ') {
					if(c > wordIdx) {
						Rectangle2D bounds = font.getStringBounds(theText, wordIdx, c, context);
						int wordW = (int) Math.round(bounds.getMaxX());
						if(wordW > min)
							min = wordW;
					}
					wordIdx = c + 1;
				} else if(ch == '\n') {
					if(c > wordIdx) {
						Rectangle2D bounds = font.getStringBounds(theText, wordIdx, c, context);
						int wordW = (int) Math.round(bounds.getMaxX());
						if(wordW > min)
							min = wordW;
					}
					wordIdx = c + 1;
					if(c > lineIdx) {
						Rectangle2D bounds = font.getStringBounds(theText, lineIdx, c, context);
						int lineW = (int) Math.round(bounds.getMaxX());
						if(lineW > max)
							max = lineW;
					}
					lineIdx = c + 1;
				}
			}
			if(wordIdx < theText.length()) {
				Rectangle2D bounds = font.getStringBounds(theText, wordIdx, theText.length(), context);
				int wordW = (int) Math.round(bounds.getMaxX());
				if(wordW > min)
					min = wordW;
			}
			if(lineIdx < theText.length()) {
				Rectangle2D bounds = font.getStringBounds(theText, lineIdx, theText.length(), context);
				int lineW = (int) Math.round(bounds.getMaxX());
				if(lineW > max)
					max = lineW;
			}
			return new SimpleSizePolicy(min, max, max, 1);
		} else {
			int w = 0;
			for(int c = 0; c < theText.length(); c++) {
				char ch = theText.charAt(c);
				if(ch == '\n') {
					if(c > lineIdx) {
						Rectangle2D bounds = font.getStringBounds(theText, lineIdx, c, context);
						int lineW = (int) Math.round(bounds.getMaxX());
						if(lineW > w)
							w = lineW;
					}
					lineIdx = c + 1;
				}
			}
			if(lineIdx < theText.length()) {
				Rectangle2D bounds = font.getStringBounds(theText, lineIdx, theText.length(), context);
				int lineW = (int) Math.round(bounds.getMaxX());
				if(lineW > w)
					w = lineW;
			}
			return new SimpleSizePolicy(w, w, w, 0);
		}
	}

	@Override
	public SizePolicy getHSizer(int width) {
		int [] min = new int[1];
		int height = render(width, null, min);
		return new SimpleSizePolicy(min[0], height, height, 1);
	}

	private int render(int width, Graphics2D graphics, int [] min) {
		if(theText.length() == 0)
			return 0;
		java.awt.Font font = MuisUtils.getFont(getStyle());
		if(font == null) {
			msg().error("Could not derive font");
			return 0;
		}
		java.awt.font.FontRenderContext context = new java.awt.font.FontRenderContext(null, getStyle().getSelf()
			.get(org.muis.core.style.FontStyle.antiAlias).booleanValue(), false);
		Font preFont = null;
		boolean preAntiAlias = false;
		int base = 0;
		if(graphics != null) {
			preFont = graphics.getFont();
			preAntiAlias = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING) == RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
			graphics.setFont(font);
			if(getStyle().getSelf().get(org.muis.core.style.FontStyle.antiAlias))
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			else
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			base = Math.round(font.getLineMetrics("AjpQq", context).getAscent());
		}
		try {
			int lineIdx = 0;
			if(getStyle().get(org.muis.core.style.FontStyle.wordWrap) && width > 0) {
				int max = 0;
				int wordIdx = 0;
				int breakIdx = 0;
				for(int c = 0; c < theText.length(); c++) {
					char ch = theText.charAt(c);
					if(ch == ' ' || ch == '\n') {
						Rectangle2D lineBounds = font.getStringBounds(theText, breakIdx, c, context);
						int lineW = (int) Math.round(lineBounds.getMaxX());
						while(lineW > width && c > breakIdx) {
							if(wordIdx > breakIdx) { // Can break at the last word
								if(graphics != null)
									graphics.drawString(theText.substring(breakIdx, wordIdx), 0, base + max);
								max += Math.round(font.getLineMetrics(theText, breakIdx, wordIdx, context).getHeight());
								breakIdx = wordIdx;
							} else { // The first word is too long--break it where we can
								int newBreak = getBreak(breakIdx, c, font, context, width);
								if(graphics != null)
									graphics.drawString(theText.substring(breakIdx, newBreak), 0, base + max);
								max += Math.round(font.getLineMetrics(theText, breakIdx, newBreak, context).getHeight());
								breakIdx = newBreak;
							}
							lineBounds = font.getStringBounds(theText, breakIdx, c, context);
							lineW = (int) Math.round(lineBounds.getMaxX());
						}
						wordIdx = c + 1;
					}
					if(ch == '\n') {
						if(breakIdx < c) {
							if(graphics != null)
								graphics.drawString(theText.substring(breakIdx), 0, base + max);
							max += Math.round(font.getLineMetrics(theText, breakIdx, c, context).getHeight());
							breakIdx = c + 1;
						}
						if(min != null)
							min[0] += Math.round(font.getLineMetrics(theText, lineIdx, c, context).getHeight());
						lineIdx = c + 1;
					}
				}
				if(wordIdx < theText.length() && width > 0) {
					Rectangle2D lineBounds = font.getStringBounds(theText, breakIdx, theText.length(), context);
					int lineW = (int) Math.round(lineBounds.getMaxX());
					while(lineW > width && breakIdx < theText.length()) {
						if(wordIdx > breakIdx) { // Can break at the last word
							if(graphics != null)
								graphics.drawString(theText.substring(breakIdx, wordIdx), 0, base + max);
							max += Math.round(font.getLineMetrics(theText, breakIdx, wordIdx, context).getHeight());
							breakIdx = wordIdx;
						} else { // The first word is too long--break it where we can
							int newBreak = getBreak(breakIdx, theText.length(), font, context, width);
							if(graphics != null)
								graphics.drawString(theText.substring(breakIdx, newBreak), 0, base + max);
							max += Math.round(font.getLineMetrics(theText, breakIdx, newBreak, context).getHeight());
						}
						lineBounds = font.getStringBounds(theText, breakIdx, theText.length(), context);
						lineW = (int) Math.round(lineBounds.getMaxX());
					}
				}
				if(min != null && lineIdx < theText.length())
					min[0] += Math.round(font.getLineMetrics(theText, lineIdx, theText.length(), context).getHeight());
				if(breakIdx < theText.length()) {
					Rectangle2D lineBounds = font.getStringBounds(theText, breakIdx, theText.length(), context);
					int lineW = (int) Math.round(lineBounds.getMaxX());
					while(lineW > width && breakIdx < theText.length()) {
						if(wordIdx > breakIdx) { // Can break at the last word
							if(graphics != null)
								graphics.drawString(theText.substring(breakIdx, wordIdx), 0, base + max);
							max += Math.round(font.getLineMetrics(theText, breakIdx, wordIdx, context).getHeight());
							breakIdx = wordIdx;
						} else { // The first word is too long--break it where we can
							int newBreak = getBreak(breakIdx, theText.length(), font, context, width);
							if(graphics != null)
								graphics.drawString(theText.substring(breakIdx, newBreak), 0, base + max);
							max += Math.round(font.getLineMetrics(theText, breakIdx, newBreak, context).getHeight());
						}
						lineBounds = font.getStringBounds(theText, breakIdx, theText.length(), context);
						lineW = (int) Math.round(lineBounds.getMaxX());
					}
					if(breakIdx < theText.length()) {
						if(graphics != null)
							graphics.drawString(theText.substring(breakIdx), 0, base + max);
						max += Math.round(font.getLineMetrics(theText, breakIdx, theText.length(), context).getHeight());
					}
				}
				return max;
			} else {
				int h = 0;
				for(int c = 0; c < theText.length(); c++) {
					char ch = theText.charAt(c);
					if(ch == '\n') {
						if(graphics != null)
							graphics.drawString(theText.substring(lineIdx, c), 0, base + h);
						int lineH = Math.round(font.getLineMetrics(theText, lineIdx, c, context).getHeight());
						h += lineH;
						lineIdx = c + 1;
					}
				}
				if(lineIdx < theText.length()) {
					if(graphics != null)
						graphics.drawString(theText.substring(lineIdx), 0, base + h);
					h += Math.round(font.getLineMetrics(theText, lineIdx, theText.length(), context).getHeight());
				}
				if(min != null)
					min[0] = h;
				return h;
			}
		} finally {
			if(graphics != null) {
				graphics.setFont(preFont);
				if(preAntiAlias)
					graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				else
					graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			}
		}
	}

	private int getBreak(int from, int to, java.awt.Font font, java.awt.font.FontRenderContext context, int width) {
		int min = from + 1, max = to;
		while(min > max) {
			int mid = (min + max + 1) / 2;
			int lineW = (int) Math.round(font.getStringBounds(theText, from, mid, context).getMaxX());
			if(lineW > width)
				max = mid - 1;
			else if(lineW < width)
				min = mid;
			else
				return mid;
		}
		return min;
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		render(getWidth(), graphics, null);
	}

	@Override
	public String toString() {
		StringBuilder ret=new  StringBuilder();
		if(getTagName()!=null)
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
		}, "\n", theText));
		if(getTagName()!=null)
			ret.append('<').append('/').append(getTagName()).append('>');
		else
			ret.append("</TEXT\u00a1>");
		return ret.toString();
	}
}

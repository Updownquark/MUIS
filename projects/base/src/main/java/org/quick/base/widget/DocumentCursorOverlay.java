package org.quick.base.widget;

import static org.quick.base.BaseConstants.States.ENABLED;
import static org.quick.core.QuickConstants.States.FOCUS;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Duration;

import org.quick.base.style.TextEditStyle;
import org.quick.core.QuickElement;
import org.quick.core.QuickTextElement;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.SelectableDocumentModel;
import org.quick.core.model.SelectableDocumentModel.SelectionChangeEvent;
import org.quick.core.style.FontStyle;
import org.quick.core.style.QuickStyle;

/** An overlay for a text editor that flashes a cursor above a document */
public class DocumentCursorOverlay extends QuickElement {
	private QuickTextElement theTextElement;

	private QuickStyle theStyleAnchor;

	private volatile BufferedImage theCursorImage;

	private volatile Point theCursorLocation;

	private volatile long theLastCursorReset;

	private boolean isFocused;
	private boolean isEnabled;

	org.quick.motion.Animation theCursorBlinkAnimation;

	/**
	 * This method must be called by the owning text editor
	 *
	 * @param text The text element that the cursor is to be displayed over
	 */
	public void setTextElement(QuickTextElement text) {
		if(theTextElement != null)
			throw new IllegalStateException("A " + getClass().getSimpleName() + "'s text element may only be set once");
		theTextElement = text;
		theCursorBlinkAnimation = new org.quick.motion.Animation() {
			@Override
			public boolean update(long time) {
				if (!theTextElement.state().is(FOCUS))
					return true;
				if (isBlinking())
					repaintCursor();
				return false;
			}

			@Override
			public long getMaxFrequency() {
				Duration blink;
				if (theStyleAnchor != null)
					blink = theStyleAnchor.get(TextEditStyle.cursorBlink).get();
				else
					blink = TextEditStyle.cursorBlink.getDefault();
				if (blink.isZero() || blink.isNegative())
					return 100;
				return blink.dividedBy(2).toMillis();
			}
		};
		theTextElement.state().observe(FOCUS).tupleV(theTextElement.state().observe(ENABLED)).value()
			.act(tuple -> enableChanged(tuple.getValue1(), tuple.getValue2()));
		QuickDocumentModel.flatten(theTextElement.getDocumentModel()).changes().act(evt -> {
			if (evt instanceof SelectionChangeEvent) {
				resetCursorImage();
				resetBlink();
			} else if (evt instanceof ContentChangeEvent)
				resetCursorImage();
		});
	}

	/** @param style The style to get the text edit properties for the cursor from */
	public void setStyleAnchor(QuickStyle style) {
		theStyleAnchor = style;
	}

	@Override
	public boolean isClickThrough(int x, int y) {
		return true;
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		if(theTextElement == null)
			return;
		if(theTextElement.state().is(FOCUS) && isCursorOn()) {
			BufferedImage cursorImage = theCursorImage;
			Point cursorLoc = theCursorLocation;
			if(cursorImage == null || cursorLoc == null) {
				theCursorImage = cursorImage = genCursorImage(graphics);
				cursorLoc = theCursorLocation;
			}
			if(area == null || area.intersects(cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight()))
				graphics.drawImage(cursorImage, cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight(), null);
		}
	}

	private void enableChanged(boolean focused, boolean enabled) {
		isFocused = focused;
		isEnabled = enabled;
		if (focused && enabled) {
			resetBlink();
			org.quick.motion.AnimationManager.get().start(theCursorBlinkAnimation);
		} else
			repaintCursor();
	}

	private boolean isCursorOn() {
		if (!isFocused)
			return false; // Always off if not focused
		if (!isEnabled)
			return true; // Always on if focused but disabled
		Duration interval;
		if(theStyleAnchor != null)
			interval = theStyleAnchor.get(TextEditStyle.cursorBlink).get();
		else
			interval = TextEditStyle.cursorBlink.getDefault();
		if (interval.isZero())
			return true;
		if (interval.isNegative())
			return false;
		if (!(theTextElement.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) theTextElement.getDocumentModel().get();
		if(doc.getSelectionAnchor() != doc.getCursor())
			return true;
		long timeDiff = System.currentTimeMillis() - theLastCursorReset;
		return (timeDiff * 2 / interval.toMillis()) % 2 == 0;
	}

	private boolean isBlinking() {
		if (!(theTextElement.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) theTextElement.getDocumentModel().get();
		if(doc.getSelectionAnchor() != doc.getCursor())
			return false;
		Duration interval;
		if(theStyleAnchor != null)
			interval = theStyleAnchor.get(TextEditStyle.cursorBlink).get();
		else
			interval = TextEditStyle.cursorBlink.getDefault();
		if (interval.isZero() || interval.isNegative())
			return false;
		return true;
	}

	private void resetBlink() {
		theLastCursorReset = System.currentTimeMillis();
	}

	private void resetCursorImage() {
		BufferedImage cursorImage = theCursorImage;
		Point cursorLoc = theCursorLocation;
		theCursorImage = null;
		if(cursorImage != null && cursorLoc != null) {
			repaint(new Rectangle(cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight()), false);
		}
	}

	private void repaintCursor() {
		BufferedImage cursorImage = theCursorImage;
		Point cursorLoc = theCursorLocation;
		if (cursorImage != null && cursorLoc != null)
			repaint(new Rectangle(cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight()), true);
		else
			repaint(null, true);
	}

	private BufferedImage genCursorImage(Graphics2D graphics) {
		if (!(theTextElement.getDocumentModel().get() instanceof SelectableDocumentModel))
			return new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		SelectableDocumentModel doc = (SelectableDocumentModel) theTextElement.getDocumentModel().get();
		QuickStyle cursorStyle = doc.getStyleAt(doc.getCursor());
		java.awt.Font cursorFont = org.quick.util.QuickUtils.getFont(cursorStyle).get();
		java.awt.font.LineMetrics metrics = cursorFont.getLineMetrics("I", graphics.getFontRenderContext());
		float ascent = metrics.getAscent();
		float width = cursorFont.getItalicAngle() * ascent;
		if(width < 0)
			width = -width;
		ascent += 2;
		width += 1;
		BufferedImage ret = new BufferedImage(Math.round(width), Math.round(ascent), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D imgGraphics = (Graphics2D) ret.getGraphics();
		imgGraphics.setColor(org.quick.util.QuickUtils.getColor(cursorStyle.get(FontStyle.color).get(),
			cursorStyle.get(FontStyle.transparency).get()));
		imgGraphics.setStroke(new java.awt.BasicStroke(cursorStyle.get(FontStyle.weight).get().floatValue()));
		if(cursorFont.getItalicAngle() > 0)
			imgGraphics.drawLine(0, (int) ascent - 1, (int) width - 1, 0);
		else
			imgGraphics.drawLine(0, 0, (int) width - 1, (int) ascent - 1);
		imgGraphics.dispose();

		boolean wordWrap = theTextElement.getStyle().get(FontStyle.wordWrap).get();
		java.awt.geom.Point2D cursorLoc2D = theTextElement.getDocumentModel().get().getLocationAt(doc.getCursor(),
			wordWrap ? theTextElement.bounds().getWidth() : Integer.MAX_VALUE);
		Point loc = new Point((int) Math.round(cursorLoc2D.getX()), (int) Math.round(cursorLoc2D.getY()));
		loc.y += metrics.getLeading();
		loc.y--;
		loc = org.quick.util.QuickUtils.relative(loc, theTextElement, this);
		theCursorLocation = loc;

		return ret;
	}
}

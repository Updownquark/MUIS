package org.quick.widget.base;

import static org.quick.base.BaseConstants.States.ENABLED;
import static org.quick.core.QuickConstants.States.FOCUS;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.time.Duration;

import org.qommons.BiTuple;
import org.quick.base.style.TextEditStyle;
import org.quick.base.widget.DocumentCursorOverlay;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickException;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.SelectableDocumentModel;
import org.quick.core.model.SelectableDocumentModel.SelectionChangeEvent;
import org.quick.core.style.FontStyle;
import org.quick.core.style.QuickStyle;
import org.quick.motion.Animation;
import org.quick.widget.core.Point;
import org.quick.widget.core.QuickTextWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.Rectangle;
import org.quick.widget.core.model.DocumentedElement;
import org.quick.widget.util.QuickWidgetUtils;

public class DocCursorOverlayWidget<E extends DocumentCursorOverlay> extends QuickWidget<E> {
	private QuickWidget<?> theEditor;
	private QuickTextWidget theTextWidget;

	private volatile BufferedImage theCursorImage;

	private volatile Point theCursorLocation;

	private volatile long theLastCursorReset;

	private boolean isFocused;
	private boolean isEnabled;

	Animation theCursorBlinkAnimation;

	@Override
	public void init(QuickWidgetDocument document, E element, QuickDefinedWidget<QuickWidgetDocument, ?> parent) throws QuickException {
		super.init(document, element, parent);
		theCursorBlinkAnimation = new Animation() {
			@Override
			public boolean update(long time) {
				if (!theEditor.getElement().state().is(FOCUS))
					return true;
				if (isBlinking())
					repaintCursor();
				return false;
			}

			@Override
			public long getMaxFrequency() {
				Duration blink;
				if (theEditor != null)
					blink = theEditor.getElement().getStyle().get(TextEditStyle.cursorBlink).get();
				else
					blink = TextEditStyle.cursorBlink.getDefault();
				if (blink.isZero() || blink.isNegative())
					return 100;
				return blink.dividedBy(2).toMillis();
			}
		};
	}

	/**
	 * This method must be called by the owning text editor
	 *
	 * @param editor The documented element governing this overlay
	 * @param text The text element that the cursor is to be displayed over
	 */
	public void setEditor(QuickWidget<?> editor, QuickTextWidget text) {
		if (theTextWidget != null)
			throw new IllegalStateException("A " + getClass().getSimpleName() + "'s text element may only be set once");
		theEditor = editor;
		theTextWidget = text;
		StateEngine state = theEditor.getElement().state();
		state.observe(FOCUS).combine(BiTuple::new, state.observe(ENABLED)).value()//
		.act(tuple -> enableChanged(tuple.getValue1(), tuple.getValue2()));
		QuickDocumentModel.flatten(((DocumentedElement) theEditor.getElement()).getDocumentModel()).changes().act(evt -> {
			if (evt instanceof SelectionChangeEvent) {
				resetCursorImage();
				resetBlink();
			} else if (evt instanceof ContentChangeEvent)
				resetCursorImage();
		});
	}

	@Override
	public boolean isClickThrough(int x, int y) {
		return true;
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		if (theTextWidget == null)
			return;
		if (isFocused && isCursorOn()) {
			BufferedImage cursorImage = theCursorImage;
			Point cursorLoc = theCursorLocation;
			if (cursorImage == null || cursorLoc == null) {
				theCursorImage = cursorImage = genCursorImage(graphics);
				cursorLoc = theCursorLocation;
			}
			if (area == null || area.intersects(cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight()))
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
		if (theEditor != null)
			interval = theEditor.getElement().getStyle().get(TextEditStyle.cursorBlink).get();
		else
			interval = TextEditStyle.cursorBlink.getDefault();
		if (interval.isZero())
			return true;
		if (interval.isNegative())
			return false;
		if (!(((DocumentedElement) theEditor.getElement()).getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) ((DocumentedElement) theEditor.getElement()).getDocumentModel().get();
		if (doc.getSelectionAnchor() != doc.getCursor())
			return true;
		long timeDiff = System.currentTimeMillis() - theLastCursorReset;
		return (timeDiff * 2 / interval.toMillis()) % 2 == 0;
	}

	private boolean isBlinking() {
		if (!(((DocumentedElement) theEditor.getElement()).getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) ((DocumentedElement) theEditor.getElement()).getDocumentModel().get();
		if (doc.getSelectionAnchor() != doc.getCursor())
			return false;
		Duration interval;
		if (theEditor != null)
			interval = theEditor.getElement().getStyle().get(TextEditStyle.cursorBlink).get();
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
		if (cursorImage != null && cursorLoc != null) {
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
		if (!(((DocumentedElement) theEditor.getElement()).getDocumentModel().get() instanceof SelectableDocumentModel))
			return new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		SelectableDocumentModel doc = (SelectableDocumentModel) ((DocumentedElement) theEditor.getElement()).getDocumentModel().get();
		QuickStyle cursorStyle = doc.getStyleAt(doc.getCursor());
		java.awt.Font cursorFont = org.quick.util.QuickUtils.getFont(cursorStyle).get();
		java.awt.font.LineMetrics metrics = cursorFont.getLineMetrics("I", graphics.getFontRenderContext());
		float ascent = metrics.getAscent();
		float width = cursorFont.getItalicAngle() * ascent;
		if (width < 0)
			width = -width;
		ascent += 2;
		width += 1;
		BufferedImage ret = new BufferedImage(Math.round(width), Math.round(ascent), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D imgGraphics = (Graphics2D) ret.getGraphics();
		imgGraphics.setColor(
			org.quick.util.QuickUtils.getColor(cursorStyle.get(FontStyle.color).get(), cursorStyle.get(FontStyle.transparency).get()));
		imgGraphics.setStroke(new java.awt.BasicStroke(cursorStyle.get(FontStyle.weight).get().floatValue()));
		if (cursorFont.getItalicAngle() > 0)
			imgGraphics.drawLine(0, (int) ascent - 1, (int) width - 1, 0);
		else
			imgGraphics.drawLine(0, 0, (int) width - 1, (int) ascent - 1);
		imgGraphics.dispose();

		boolean wordWrap = theEditor.getElement().getStyle().get(FontStyle.wordWrap).get();
		Point2D cursorLoc2D = ((DocumentedElement) theEditor).getRenderableDocument()
			.getLocationAt(doc.getCursor(), wordWrap ? theTextWidget.bounds().getWidth() : Integer.MAX_VALUE);
		int locX = (int) Math.round(cursorLoc2D.getX());
		int locY = (int) Math.round(cursorLoc2D.getY());
		locY += metrics.getLeading();
		locY--;
		theCursorLocation = QuickWidgetUtils.relative(new Point(locX, locY), theTextWidget, this);

		return ret;
	}
}

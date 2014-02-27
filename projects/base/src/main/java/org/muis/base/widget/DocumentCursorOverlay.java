package org.muis.base.widget;

import static org.muis.core.MuisConstants.States.FOCUS;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.muis.core.MuisElement;
import org.muis.core.MuisTextElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.mgr.MuisState;
import org.muis.core.model.SimpleDocumentModel;
import org.muis.core.model.SimpleDocumentModel.ContentChangeEvent;
import org.muis.core.model.SimpleDocumentModel.SelectionChangeEvent;
import org.muis.core.style.FontStyle;

/** An overlay for a text editor that flashes a cursor above a document */
public class DocumentCursorOverlay extends MuisElement {
	private MuisTextElement theTextElement;

	private volatile BufferedImage theCursorImage;

	private volatile Point theCursorLocation;

	private volatile long theLastCursorReset;

	/**
	 * This method must be called by the owning text editor
	 * 
	 * @param text The text element that the cursor is to be displayed over
	 */
	public void setTextElement(MuisTextElement text) {
		if(theTextElement != null)
			throw new IllegalStateException("A " + getClass().getSimpleName() + "'s text element may only be set once");
		theTextElement = text;
		theTextElement.state().addListener(FOCUS, new org.muis.core.mgr.StateEngine.StateListener() {
			org.muis.motion.Animation theCursorBlinkAnimation = new org.muis.motion.Animation() {
				@Override
				public boolean update(long time) {
					if(!theTextElement.state().is(FOCUS))
						return true;
					if(isBlinking())
						repaintCursor();
					return false;
				}

				@Override
				public long getMaxFrequency() {
					long blink = getStyle().getSelf().get(org.muis.base.style.TextEditStyle.cursorBlink);
					if(blink <= 0)
						return 100;
					return blink / 2;
				}
			};

			private void repaintCursor() {
				BufferedImage cursorImage = theCursorImage;
				Point cursorLoc = theCursorLocation;
				if(cursorImage != null && cursorLoc != null) {
					repaint(new Rectangle(cursorLoc.x, cursorLoc.y, cursorImage.getWidth(), cursorImage.getHeight()), true);
				} else
					repaint(null, true);
			}

			@Override
			public void entered(MuisState state, MuisEvent<?> cause) {
				resetBlink();
				org.muis.motion.AnimationManager.get().start(theCursorBlinkAnimation);
			}

			@Override
			public void exited(MuisState state, MuisEvent<?> cause) {
				repaintCursor();
			}
		});
		theTextElement.getDocumentModel().addContentListener(new SimpleDocumentModel.ContentListener() {
			@Override
			public void contentChanged(ContentChangeEvent evt) {
				if(evt.getIndex() == theTextElement.getDocumentModel().getCursor())
					resetCursorImage();
			}
		});
		theTextElement.getDocumentModel().addSelectionListener(new SimpleDocumentModel.SelectionListener() {
			@Override
			public void selectionChanged(SelectionChangeEvent evt) {
				resetCursorImage();
				resetBlink();
			}
		});
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

	private boolean isCursorOn() {
		long interval = getStyle().getSelf().get(org.muis.base.style.TextEditStyle.cursorBlink);
		if(interval == 0)
			return true;
		if(interval < 0)
			return false;
		if(theTextElement.getDocumentModel().getSelectionAnchor() != theTextElement.getDocumentModel().getCursor())
			return true;
		long timeDiff = System.currentTimeMillis() - theLastCursorReset;
		return (timeDiff * 2 / interval) % 2 == 0;
	}

	private boolean isBlinking() {
		if(theTextElement.getDocumentModel().getSelectionAnchor() != theTextElement.getDocumentModel().getCursor())
			return false;
		long interval = getStyle().getSelf().get(org.muis.base.style.TextEditStyle.cursorBlink);
		if(interval <= 0)
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

	private BufferedImage genCursorImage(Graphics2D graphics) {
		org.muis.core.style.MuisStyle cursorStyle = theTextElement.getDocumentModel().getStyleAt(
			theTextElement.getDocumentModel().getCursor());
		java.awt.Font cursorFont = org.muis.util.MuisUtils.getFont(cursorStyle);
		java.awt.font.LineMetrics metrics = cursorFont.getLineMetrics("I", graphics.getFontRenderContext());
		float ascent = metrics.getAscent();
		float width = cursorFont.getItalicAngle() * ascent;
		if(width < 0)
			width = -width;
		ascent += 2;
		width += 1;
		BufferedImage ret = new BufferedImage(Math.round(width), Math.round(ascent), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D imgGraphics = (Graphics2D) ret.getGraphics();
		imgGraphics.setColor(org.muis.util.MuisUtils.getColor(cursorStyle.get(FontStyle.color), cursorStyle.get(FontStyle.transparency)));
		imgGraphics.setStroke(new java.awt.BasicStroke(cursorStyle.get(FontStyle.weight).floatValue()));
		if(cursorFont.getItalicAngle() > 0)
			imgGraphics.drawLine(0, (int) ascent - 1, (int) width - 1, 0);
		else
			imgGraphics.drawLine(0, 0, (int) width - 1, (int) ascent - 1);
		imgGraphics.dispose();

		java.awt.geom.Point2D cursorLoc2D = theTextElement.getDocumentModel().getLocationAt(theTextElement.getDocumentModel().getCursor(),
			theTextElement.bounds().getWidth());
		Point loc = new Point((int) Math.round(cursorLoc2D.getX()), (int) Math.round(cursorLoc2D.getY()));
		loc.y += metrics.getLeading();
		loc.y--;
		loc = org.muis.util.MuisUtils.relative(loc, theTextElement, this);
		theCursorLocation = loc;

		return ret;
	}
}

package org.muis.test.widget;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.muis.core.event.BoundsChangedEvent;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.event.MouseEvent;
import org.muis.core.event.MuisEventListener;

/**
 * Provides testing feedback for mouse events--paints the font color over pixels where the mouse passed over while the left button was
 * pressed
 */
public class PaintWidget extends org.muis.base.widget.Block {
	java.awt.image.BufferedImage theImage;

	/** Creates a PaintWidget */
	public PaintWidget() {
		setFocusable(true);
		events().listen(BoundsChangedEvent.bounds, new MuisEventListener<BoundsChangedEvent>() {
			@Override
			public void eventOccurred(BoundsChangedEvent event) {
				resized();
			}
		}).listen(MouseEvent.mouse, new MuisEventListener<MouseEvent>() {
			private boolean isMouseDown;

			@Override
			public void eventOccurred(MouseEvent event) {
				switch (event.getType()) {
				case entered:
					isMouseDown = getDocument().isButtonPressed(MouseEvent.ButtonType.left);
					break;
				case exited:
					drawPoint(event);
					break;
				case pressed:
					if(event.getButton() == MouseEvent.ButtonType.left) {
						isMouseDown = true;
						drawPoint(event);
					}
					break;
				case released:
					if(event.getButton() == MouseEvent.ButtonType.left) {
						drawPoint(event);
						isMouseDown = false;
					}
					break;
				default:
				}
			}

			void drawPoint(MouseEvent mEvt) {
				if(isMouseDown) {
					Point pos = mEvt.getPosition(PaintWidget.this);
					try {
						theImage.setRGB(pos.x, pos.y, getStyle().getSelf().get(org.muis.core.style.FontStyle.color).getRGB());
					} catch(ArrayIndexOutOfBoundsException e) {
						System.err.println("Position " + pos + " out of bounds (" + bounds().getWidth() + "x" + bounds().getHeight() + ")");
					}
					repaint(new Rectangle(pos.x, pos.y, 1, 1), true);
				}
			}
		}).listen(KeyBoardEvent.key.press(), new MuisEventListener<KeyBoardEvent>() {
			@Override
			public void eventOccurred(KeyBoardEvent event) {
				if(event.getKeyCode() == KeyBoardEvent.KeyCode.SPACE && theImage != null) {
					for(int x = 0; x < theImage.getWidth(); x++) {
						for(int y = 0; y < theImage.getHeight(); y++) {
							theImage.setRGB(x, y, 0);
						}
					}
				}
				repaint(null, true);
			}
		});
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		if(theImage != null) {
			graphics.drawImage(theImage, 0, 0, theImage.getWidth(), theImage.getHeight(),
				getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.color), null);
		}
	}

	void resized() {
		if(theImage == null && bounds().getWidth() > 0 && bounds().getHeight() > 0) {
			theImage = new java.awt.image.BufferedImage(bounds().getWidth(), bounds().getHeight(),
				java.awt.image.BufferedImage.TYPE_4BYTE_ABGR);
			repaint(null, false);
		} else if(theImage != null && bounds().getWidth() > theImage.getWidth() || bounds().getHeight() > theImage.getHeight()) {
			int w = bounds().getWidth() > theImage.getWidth() ? bounds().getWidth() : theImage.getWidth();
			int h = bounds().getHeight() > theImage.getHeight() ? bounds().getHeight() : theImage.getHeight();
			java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_4BYTE_ABGR);
			img.getGraphics().drawImage(theImage, 0, 0, theImage.getWidth(), theImage.getHeight(),
				getStyle().getSelf().get(org.muis.core.style.BackgroundStyle.color), null);
			repaint(null, false);
		}
	}
}

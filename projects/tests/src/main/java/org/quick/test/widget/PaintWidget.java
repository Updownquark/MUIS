package org.quick.test.widget;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import org.quick.core.event.BoundsChangedEvent;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.MouseEvent;

/**
 * Provides testing feedback for mouse events--paints the font color over pixels where the mouse passed over while the left button was
 * pressed
 */
public class PaintWidget extends org.quick.base.widget.Block {
	java.awt.image.BufferedImage theImage;

	/** Creates a PaintWidget */
	public PaintWidget() {
		setFocusable(true);
		events().filterMap(BoundsChangedEvent.bounds).act(event -> {
			resized();
		});
		events().filterMap(MouseEvent.mouse).act(new org.observe.Action<MouseEvent>() {
			private boolean isMouseDown;

			@Override
			public void act(MouseEvent event) {
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
						theImage.setRGB(pos.x, pos.y, getStyle().get(org.quick.core.style.FontStyle.color).get().getRGB());
					} catch(ArrayIndexOutOfBoundsException e) {
						System.err.println("Position " + pos + " out of bounds (" + bounds().getWidth() + "x" + bounds().getHeight() + ")");
					}
					repaint(new Rectangle(pos.x, pos.y, 1, 1), true);
				}
			}
		});
		events().filterMap(KeyBoardEvent.key.press()).act(event -> {
			if(event.getKeyCode() == KeyBoardEvent.KeyCode.SPACE && theImage != null) {
				for(int x = 0; x < theImage.getWidth(); x++) {
					for(int y = 0; y < theImage.getHeight(); y++) {
						theImage.setRGB(x, y, 0);
					}
				}
			}
			repaint(null, true);
		});
	}

	@Override
	public void paintSelf(Graphics2D graphics, Rectangle area) {
		super.paintSelf(graphics, area);
		if(theImage != null) {
			graphics.drawImage(theImage, 0, 0, theImage.getWidth(), theImage.getHeight(),
				getStyle().get(org.quick.core.style.BackgroundStyle.color).get(), null);
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
				getStyle().get(org.quick.core.style.BackgroundStyle.color).get(), null);
			repaint(null, false);
		}
	}
}

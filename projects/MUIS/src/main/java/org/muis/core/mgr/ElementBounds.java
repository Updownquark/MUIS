package org.muis.core.mgr;

import static org.muis.core.MuisConstants.Events.BOUNDS_CHANGED;

import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisPropertyEvent;
import org.muis.core.layout.Orientation;
import org.muis.core.layout.SizePolicy;

public class ElementBounds implements org.muis.core.layout.Bounds {
	private final MuisElement theElement;

	private ElementBoundsDimension theHorizontalBounds;

	private ElementBoundsDimension theVerticalBounds;

	private int theX;

	private int theY;

	private int theW;

	private int theH;

	public ElementBounds(MuisElement element) {
		theElement = element;
		theHorizontalBounds = new ElementBoundsDimension(false);
		theVerticalBounds = new ElementBoundsDimension(true);
	}

	public ElementBoundsDimension getHorizontal() {
		return theHorizontalBounds;
	}

	public ElementBoundsDimension h() {
		return theHorizontalBounds;
	}

	public ElementBoundsDimension getVertical() {
		return theVerticalBounds;
	}

	public ElementBoundsDimension v() {
		return theVerticalBounds;
	}

	/**
	 * @see org.muis.core.layout.Bounds#get(org.muis.core.layout.Orientation)
	 */
	@Override
	public ElementBoundsDimension get(Orientation orientation) {
		switch (orientation) {
		case horizontal:
			return theHorizontalBounds;
		case vertical:
			return theVerticalBounds;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orientation);
	}

	public int getX() {
		return theX;
	}

	public void setX(int x) {
		if(theX == x)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theX = x;
		theElement.fireEvent(new MuisPropertyEvent<>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false, false);
		theHorizontalBounds.setPosition(x);
	}

	public int getY() {
		return theY;
	}

	public void setY(int y) {
		if(theY == y)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theY = y;
		theElement.fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false,
			false);
	}

	public java.awt.Point getPosition() {
		return new java.awt.Point(theX, theY);
	}

	public void setPosition(int x, int y) {
		if(theX == x && theY == y)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theX = x;
		theY = y;
		theElement.fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false,
			false);
	}

	public int getWidth() {
		return theW;
	}

	public void setWidth(int width) {
		if(theW == width)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theW = width;
		theElement.fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false,
			false);
	}

	public int getHeight() {
		return theH;
	}

	public void setHeight(int height) {
		if(theH == height)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theH = height;
		theElement.fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false,
			false);
	}

	public java.awt.Dimension getSize() {
		return new java.awt.Dimension(theW, theH);
	}

	public void setSize(int width, int height) {
		if(theW == width && theH == height)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theW = width;
		theH = height;
		theElement.fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false,
			false);
	}

	public Rectangle getBounds() {
		return new Rectangle(theX, theY, theW, theH);
	}

	public void setBounds(int x, int y, int width, int height) {
		if(theX == x && theY == y && theW == width && theH == height)
			return;
		Rectangle preBounds = new Rectangle(theX, theY, theW, theH);
		theX = x;
		theY = y;
		theW = width;
		theH = height;
		theElement.fireEvent(new MuisPropertyEvent<Rectangle>(BOUNDS_CHANGED, preBounds, new Rectangle(theX, theY, theW, theH)), false,
			false);
	}

	public class ElementBoundsDimension implements org.muis.core.layout.BoundsDimension {
		private boolean isVertical;

		ElementBoundsDimension(boolean vertical) {
			isVertical = vertical;
		}

		@Override
		public int getPosition() {
			return isVertical ? theY : theX;
		}

		@Override
		public void setPosition(int pos) {
			if(isVertical)
				setY(pos);
			else
				setX(pos);
		}

		@Override
		public int getSize() {
			return isVertical ? theH : theW;
		}

		@Override
		public void setSize(int size) {
			if(isVertical)
				setHeight(size);
			else
				setWidth(size);
		}

		@Override
		public SizePolicy getGuide() {
			return isVertical ? theElement.getHSizer() : theElement.getWSizer();
		}
	}
}

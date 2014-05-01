package org.muis.core;

import java.awt.Point;
import java.util.List;

/** Contains extra information pertaining to the location of a positioned event over the document */
public class MuisEventPositionCapture extends MuisElementCapture {
	private final int theEventX;

	private final int theEventY;

	/**
	 * @param p The parent structure
	 * @param el The MUIS element that this structure is a capture of
	 * @param xPos The x-coordinate of the element's upper-left corner
	 * @param yPos The y-coordinate of the element's upper-left corner
	 * @param zIndex The z-index of the element
	 * @param w The width of the element
	 * @param h The height of the element
	 * @param evtX The x-coordinate of the positioned event relative to this element
	 * @param evtY The y-coordinate of the positioned event relative to this element
	 */
	public MuisEventPositionCapture(MuisEventPositionCapture p, MuisElement el, int xPos, int yPos, int zIndex, int w, int h, int evtX,
		int evtY) {
		super(p, el, xPos, yPos, zIndex, w, h);
		theEventX = evtX;
		theEventY = evtY;
	}

	/** @return The x-coordinate of the positioned event relative to this element */
	public int getEventX() {
		return theEventX;
	}

	/** @return The y-coordinate of the positioned event relative to this element */
	public int getEventY() {
		return theEventY;
	}

	public boolean isClickThrough(Point pos) {
		if(!getElement().isClickThrough(pos.x, pos.y))
			return false;
		for(MuisEventPositionCapture child : getChildren()) {
			Point childPos = getChildIntersection(child, pos);
			if(!child.isClickThrough(childPos))
				return false;
		}
		return true;
	}

	@Override
	public void addChild(MuisElementCapture child) throws SealedException {
		if(!(child instanceof MuisEventPositionCapture))
			throw new IllegalArgumentException(MuisEventPositionCapture.class.getName() + " may only contain other "
				+ MuisEventPositionCapture.class.getName() + " instances as children");
		super.addChild(child);
	}

	@Override
	public MuisEventPositionCapture getChild(int index) {
		return (MuisEventPositionCapture) super.getChild(index);
	}

	@Override
	public List<? extends MuisEventPositionCapture> getChildren() {
		return (List<? extends MuisEventPositionCapture>) super.getChildren();
	}

	@Override
	public Iterable<? extends MuisEventPositionCapture> getTargets() {
		return (Iterable<? extends MuisEventPositionCapture>) super.getTargets();
	}

	@Override
	public Iterable<? extends MuisEventPositionCapture> iterate(boolean depthFirst) {
		return (Iterable<? extends MuisEventPositionCapture>) super.iterate(depthFirst);
	}

	@Override
	public MuisEventPositionCapture find(MuisElement el) {
		return (MuisEventPositionCapture) super.find(el);
	}

	@Override
	public MuisEventPositionCapture getTarget() {
		return (MuisEventPositionCapture) super.getTarget();
	}

	@Override
	public MuisEventPositionCapture getRoot() {
		return (MuisEventPositionCapture) super.getRoot();
	}

	@Override
	public MuisEventPositionCapture getParent() {
		return (MuisEventPositionCapture) super.getParent();
	}

	@Override
	public void setParent(MuisElementCapture parent) {
		if(!(parent instanceof MuisEventPositionCapture))
			throw new IllegalArgumentException(MuisEventPositionCapture.class.getName() + " must have a "
				+ MuisEventPositionCapture.class.getName() + " instance as a parent");
		super.setParent(parent);
	}

	@Override
	protected MuisEventPositionCapture clone() {
		return (MuisEventPositionCapture) super.clone();
	}
}

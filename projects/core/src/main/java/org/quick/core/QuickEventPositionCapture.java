package org.quick.core;

import java.util.List;

/** Contains extra information pertaining to the location of a positioned event over the document */
public class QuickEventPositionCapture extends QuickElementCapture {
	private final int theEventX;

	private final int theEventY;

	/**
	 * @param p The parent structure
	 * @param el The Quick element that this structure is a capture of
	 * @param xPos The x-coordinate of the element's upper-left corner
	 * @param yPos The y-coordinate of the element's upper-left corner
	 * @param zIndex The z-index of the element
	 * @param w The width of the element
	 * @param h The height of the element
	 * @param evtX The x-coordinate of the positioned event relative to this element
	 * @param evtY The y-coordinate of the positioned event relative to this element
	 */
	public QuickEventPositionCapture(QuickEventPositionCapture p, QuickElement el, int xPos, int yPos, int zIndex, int w, int h, int evtX,
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

	@Override
	public void addChild(QuickElementCapture child) throws SealedException {
		if(!(child instanceof QuickEventPositionCapture))
			throw new IllegalArgumentException(QuickEventPositionCapture.class.getName() + " may only contain other "
				+ QuickEventPositionCapture.class.getName() + " instances as children");
		super.addChild(child);
	}

	@Override
	public QuickEventPositionCapture getChild(int index) {
		return (QuickEventPositionCapture) super.getChild(index);
	}

	@Override
	public List<? extends QuickEventPositionCapture> getChildren() {
		return (List<? extends QuickEventPositionCapture>) super.getChildren();
	}

	@Override
	public Iterable<? extends QuickEventPositionCapture> getTargets() {
		return (Iterable<? extends QuickEventPositionCapture>) super.getTargets();
	}

	@Override
	public Iterable<? extends QuickEventPositionCapture> iterate(boolean depthFirst) {
		return (Iterable<? extends QuickEventPositionCapture>) super.iterate(depthFirst);
	}

	@Override
	public QuickEventPositionCapture find(QuickElement el) {
		return (QuickEventPositionCapture) super.find(el);
	}

	@Override
	public QuickEventPositionCapture getTarget() {
		return (QuickEventPositionCapture) super.getTarget();
	}

	@Override
	public QuickEventPositionCapture getRoot() {
		return (QuickEventPositionCapture) super.getRoot();
	}

	@Override
	public QuickEventPositionCapture getParent() {
		return (QuickEventPositionCapture) super.getParent();
	}

	@Override
	public void setParent(QuickElementCapture parent) {
		if(!(parent instanceof QuickEventPositionCapture))
			throw new IllegalArgumentException(QuickEventPositionCapture.class.getName() + " must have a "
				+ QuickEventPositionCapture.class.getName() + " instance as a parent");
		super.setParent(parent);
	}

	@Override
	protected QuickEventPositionCapture clone() {
		return (QuickEventPositionCapture) super.clone();
	}
}

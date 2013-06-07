package org.muis.core;

public class MuisEventPositionCapture<C extends MuisEventPositionCapture<C>> extends MuisElementCapture<C> {
	private final int theEventX;

	private final int theEventY;

	public MuisEventPositionCapture(C aParent, MuisElement el, int xPos, int yPos, int zIndex, int w, int h, int evtX, int evtY) {
		super(aParent, el, xPos, yPos, zIndex, w, h);
		theEventX = evtX;
		theEventY = evtY;
	}

	public int getEventX() {
		return theEventX;
	}

	public int getEventY() {
		return theEventY;
	}
}

package org.muis.core;

/**
 * Contains extra information pertaining to the location of a positioned event over the document
 *
 * @param <C> The sub type of the capture
 */
public class MuisEventPositionCapture<C extends MuisEventPositionCapture<C>> extends MuisElementCapture<C> {
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
	public MuisEventPositionCapture(C p, MuisElement el, int xPos, int yPos, int zIndex, int w, int h, int evtX, int evtY) {
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
}

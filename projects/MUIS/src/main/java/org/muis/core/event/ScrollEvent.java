package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

/**
 * Represents a scrolling action by the user. The position and element fields may depend on the value of
 * {@link org.muis.core.MuisDocument#getScrollPolicy()}.
 *
 * @see org.muis.core.MuisDocument.ScrollPolicy#MOUSE
 * @see org.muis.core.MuisDocument.ScrollPolicy#FOCUS
 * @see org.muis.core.MuisDocument.ScrollPolicy#MIXED
 */
public class ScrollEvent extends PositionedUserEvent {
	/** Different types of scroll actions that may cause a ScrollEvent */
	public static enum ScrollType {
		/** Represents scrolling by a small unit, e.g. arrow keys or mouse wheel */
		UNIT,
		/** Represents scrolling by a block on the page, e.g. Pg Up/Pg Dn */
		BLOCK;
	}

	private final ScrollEvent theBacking;

	private final ScrollType theScrollType;

	private final boolean isVertical;

	private final KeyBoardEvent theKeyEvent;

	private final int theAmount;

	/**
	 * Creates a scroll event
	 *
	 * @param doc The document that event occurred in
	 * @param target The element that is the target of the scroll event
	 * @param scrollType The type of scroll that generated this event
	 * @param vertical Whether this scroll event represents a vertical or a horizontal scrolling action
	 * @param amount The amount of {@link ScrollType} units that caused this event
	 * @param keyEvent The key event that caused this scroll event
	 * @param capture The capture of the event's location on each element relevant to it
	 */
	public ScrollEvent(MuisDocument doc, MuisElement target, ScrollType scrollType, boolean vertical, int amount, KeyBoardEvent keyEvent,
		org.muis.core.MuisEventPositionCapture<?> capture) {
		super(doc, target, target, System.currentTimeMillis(), capture);
		theBacking = null;
		theScrollType = scrollType;
		isVertical = vertical;
		theKeyEvent = keyEvent;
		theAmount = amount;
	}

	private ScrollEvent(ScrollEvent backing, org.muis.core.MuisEventPositionCapture<?> capture) {
		super(backing.getDocument(), backing.getTarget(), capture.getElement(), backing.getTime(), capture);
		theBacking = backing;
		theScrollType = backing.theScrollType;
		isVertical = backing.isVertical;
		theKeyEvent = backing.theKeyEvent;
		theAmount = backing.theAmount;
	}

	/** @return The type of scrolling that generated this event */
	public ScrollType getScrollType() {
		return theScrollType;
	}

	/** @return Whether this scroll event represents a vertical or a horizontal scrolling action */
	public boolean isVertical() {
		return isVertical;
	}

	/** @return The key event that caused this scroll event, or null if this scroll event was not caused by a key event */
	public KeyBoardEvent getKeyEvent() {
		return theKeyEvent;
	}

	/**
	 * This method returns the number of "clicks" on the mouse wheel that generated this event. Alternatively, this will be 1 for block
	 * scrolling or arrow key scrolling.
	 *
	 * @return The number of units or blocks to scroll for this event
	 */
	public int getAmount() {
		return theAmount;
	}

	@Override
	public ScrollEvent copyFor(org.muis.core.MuisElement element) {
		if(getCapture() == null)
			return this;
		org.muis.core.MuisEventPositionCapture<?> capture = getCapture().find(element);
		if(capture == null)
			throw new IllegalArgumentException("This event (" + this + ") is not relevant to the given element (" + element + ")");
		return new ScrollEvent(this, capture);
	}

	@Override
	public boolean isUsed() {
		if(theBacking != null)
			return theBacking.isUsed();
		else
			return super.isUsed();
	}

	@Override
	public void use() {
		if(theBacking != null)
			theBacking.use();
		else
			super.use();
	}

	@Override
	public String toString() {
		return "Scrolled " + theAmount + " " + theScrollType + (isVertical ? " " : " horizontally ") + " at " + getElement();
	}
}

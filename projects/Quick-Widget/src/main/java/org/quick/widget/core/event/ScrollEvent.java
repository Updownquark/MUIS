package org.quick.widget.core.event;

import java.util.Set;

import org.quick.widget.core.QuickEventPositionCapture;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

/**
 * Represents a scrolling action by the user. The position and element fields may depend on the value of
 * {@link QuickWidgetDocument#getScrollPolicy()}.
 *
 * @see org.quick.widget.core.QuickWidgetDocument.ScrollPolicy#MOUSE
 * @see org.quick.widget.core.QuickWidgetDocument.ScrollPolicy#FOCUS
 * @see org.quick.widget.core.QuickWidgetDocument.ScrollPolicy#MIXED
 */
public class ScrollEvent extends PositionedUserEvent {
	/** Different types of scroll actions that may cause a ScrollEvent */
	public static enum ScrollType {
		/** Represents scrolling by a small unit, e.g. arrow keys or mouse wheel */
		UNIT,
		/** Represents scrolling by a block on the page, e.g. Pg Up/Pg Dn */
		BLOCK;
	}

	private final UserEvent theCause;

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
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param capture The capture of the event's location on each element relevant to it
	 * @param cause The user event that may have triggered this scroll event
	 */
	public ScrollEvent(QuickWidgetDocument doc, QuickWidget target, ScrollType scrollType, boolean vertical, int amount,
		KeyBoardEvent keyEvent,
		Set<MouseEvent.ButtonType> pressedButtons, Set<KeyBoardEvent.KeyCode> pressedKeys, QuickEventPositionCapture capture,
		UserEvent cause) {
		super(doc, target, target, pressedButtons, pressedKeys, System.currentTimeMillis(), capture, cause);
		theCause = cause;
		theBacking = null;
		theScrollType = scrollType;
		isVertical = vertical;
		theKeyEvent = keyEvent;
		theAmount = amount;
	}

	private ScrollEvent(ScrollEvent backing, QuickEventPositionCapture capture) {
		super(backing.getDocument(), backing.getTarget(), capture.getWidget(), backing.getPressedButtons(), backing.getPressedKeys(),
			backing.getTime(), capture, backing.getCause());
		theCause = backing.getCause();
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

	@Override
	public UserEvent getCause() {
		return theCause;
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
	public ScrollEvent copyFor(QuickWidget element) {
		if(getCapture() == null)
			return this;
		QuickEventPositionCapture capture = getCapture().find(element);
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
		return "Scrolled " + theAmount + " " + theScrollType + (isVertical ? " " : " horizontally ") + " at " + getWidget();
	}
}

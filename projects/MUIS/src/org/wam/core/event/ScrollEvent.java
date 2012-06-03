package org.wam.core.event;

/**
 * Represents a scrolling action by the user. The position and element fields may depend on the
 * value of {@link org.wam.core.WamDocument#getScrollPolicy()}.
 * 
 * @see org.wam.core.WamDocument.ScrollPolicy#MOUSE
 * @see org.wam.core.WamDocument.ScrollPolicy#FOCUS
 * @see org.wam.core.WamDocument.ScrollPolicy#MIXED
 */
public class ScrollEvent extends PositionedUserEvent
{
	/**
	 * Different types of scroll actions that may cause a ScrollEvent
	 */
	public static enum ScrollType
	{
		/**
		 * Represents scrolling by a small unit, e.g. arrow keys or mouse wheel
		 */
		UNIT,
		/**
		 * Represents scrolling by a block on the page, e.g. Pg Up/Pg Dn
		 */
		BLOCK;
	}

	private final ScrollType theScrollType;

	private final boolean isVertical;

	private final KeyBoardEvent theKeyEvent;

	private final int theAmount;

	/**
	 * Creates a scroll event
	 * 
	 * @param doc The document that event occurred in
	 * @param element The element that is the target of the scroll event
	 * @param docX The x-position at which the event originated
	 * @param docY The y-position at which the event originated
	 * @param scrollType The type of scroll that generated this event
	 * @param vertical Whether this scroll event represents a vertical or a horizontal scrolling
	 *            action
	 * @param amount The amount of {@link ScrollType} units that caused this event
	 * @param keyEvent The key event that caused this scroll event
	 */
	public ScrollEvent(org.wam.core.WamDocument doc, org.wam.core.WamElement element, int docX,
		int docY, ScrollType scrollType, boolean vertical, int amount, KeyBoardEvent keyEvent)
	{
		super(org.wam.core.WamElement.SCROLL_EVENT, doc, element, docX, docY);
		theScrollType = scrollType;
		isVertical = vertical;
		theKeyEvent = keyEvent;
		theAmount = amount;
	}

	/**
	 * @return The type of scrolling that generated this event
	 */
	public ScrollType getScrollType()
	{
		return theScrollType;
	}

	/**
	 * @return Whether this scroll event represents a vertical or a horizontal scrolling action
	 */
	public boolean isVertical()
	{
		return isVertical;
	}

	/**
	 * @return The key event that caused this scroll event, or null if this scroll event was not
	 *         caused by a key event
	 */
	public KeyBoardEvent getKeyEvent()
	{
		return theKeyEvent;
	}

	/**
	 * This method returns the number of "clicks" on the mouse wheel that generated this event.
	 * Alternatively, this will be 1 for block scrolling or arrow key scrolling.
	 * 
	 * @return The number of units or blocks to scroll for this event
	 */
	public int getAmount()
	{
		return theAmount;
	}
}

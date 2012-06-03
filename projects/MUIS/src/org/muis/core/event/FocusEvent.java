package org.muis.core.event;

/**
 * An event that occurs when the user changes focus
 */
public class FocusEvent extends UserEvent
{
	private final boolean isFocus;

	/**
	 * Creates a FocusEvent
	 * 
	 * @param doc The document that the event was fired on
	 * @param element The element that gained or lost the document's focus
	 * @param focus Whether the element gained the focus (true) or lost the focus (false)
	 */
	public FocusEvent(org.muis.core.MuisDocument doc, org.muis.core.MuisElement element, boolean focus)
	{
		super(org.muis.core.MuisElement.FOCUS_EVENT, doc, element);
		isFocus = focus;
	}

	/**
	 * @return Whether this event represents the element coming into focus (true) or out of focus
	 *         (false)
	 */
	public boolean isFocus()
	{
		return isFocus;
	}
}

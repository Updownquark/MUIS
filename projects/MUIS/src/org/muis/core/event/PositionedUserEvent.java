package org.muis.core.event;

import org.muis.core.MuisElement;

/**
 * An event caused by user interaction for which an x,y point location is relevant
 */
public class PositionedUserEvent extends UserEvent
{
	private final int theDocumentX;

	private final int theDocumentY;

	/**
	 * Creates a positioned user event
	 * 
	 * @param type The event type that this event is an instance of
	 * @param doc The document that the mouse event occurred in
	 * @param element The deepest-level element that the event occurred in
	 * @param docX The absolute x-coordinate of the event relative to the document's root element
	 * @param docY The absolute y-coordinate of the event relative to the document's root element
	 */
	public PositionedUserEvent(MuisEventType<Void> type, org.muis.core.MuisDocument doc,
		MuisElement element, int docX, int docY)
	{
		super(type, doc, element);
		theDocumentX = docX;
		theDocumentY = docY;
	}

	/**
	 * @return The absolute x-coordinate of the event relative to the document's root element
	 */
	public int getX()
	{
		return theDocumentX;
	}

	/**
	 * @return The absolute y-coordinate of the event relative to the document's root element
	 */
	public int getY()
	{
		return theDocumentY;
	}

	/**
	 * @param element The element to get the relative position of this event
	 * @return The position at which this event occurred relative to the upper-left corner of the
	 *         given element
	 */
	public java.awt.Point getRelativePosition(MuisElement element)
	{
		int x = theDocumentX;
		int y = theDocumentY;
		MuisElement last = element;
		while(element != null)
		{
			x -= element.getX();
			y -= element.getY();
			last = element;
			element = element.getParent();
		}
		if(last.getDocument() != getDocument())
			throw new IllegalArgumentException(
				"This event and the element are not from the same document");
		return new java.awt.Point(x, y);
	}
}

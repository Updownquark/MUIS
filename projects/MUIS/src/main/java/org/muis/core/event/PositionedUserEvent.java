package org.muis.core.event;

import org.muis.core.MuisElement;

/** An event caused by user interaction for which an x,y point location is relevant */
public class PositionedUserEvent extends UserEvent
{
	private final int theDocumentX;

	private final int theDocumentY;

	private org.muis.core.MuisElementCapture theCapture;

	/**
	 * Creates a positioned user event
	 *
	 * @param type The event type that this event is an instance of
	 * @param doc The document that the mouse event occurred in
	 * @param element The deepest-level element that the event occurred in
	 * @param docX The absolute x-coordinate of the event relative to the document's root element
	 * @param docY The absolute y-coordinate of the event relative to the document's root element
	 * @param capture The capture of the event's location on each element relevant to it
	 */
	public PositionedUserEvent(MuisEventType<Void> type, org.muis.core.MuisDocument doc, MuisElement element, int docX, int docY,
		org.muis.core.MuisElementCapture capture)
	{
		super(type, doc, element);
		theDocumentX = docX;
		theDocumentY = docY;
		theCapture = capture;
		if(theCapture != null)
			theCapture.seal();
	}

	/** @return The absolute x-coordinate of the event relative to the document's root element */
	public int getX()
	{
		return theDocumentX;
	}

	/** @return The absolute y-coordinate of the event relative to the document's root element */
	public int getY()
	{
		return theDocumentY;
	}

	/** @return The capture of all elements that this event might be relevant to */
	public org.muis.core.MuisElementCapture getCapture()
	{
		return theCapture;
	}

	/**
	 * @param element The element to get the position of for this event
	 * @return The position of this event over the given element, or null if this information cannot be obtained from this event's capture
	 */
	public java.awt.Point getPosition(MuisElement element)
	{
		if(theCapture == null)
			return null;
		org.muis.core.MuisElementCapture capture = theCapture.find(element);
		if(capture == null)
			for(org.muis.core.MuisElementCapture mec : theCapture.iterate(false))
				if(mec.element == element)
				{
					capture = mec;
					break;
				}
		return capture == null ? null : new java.awt.Point(capture.x, capture.y);
	}
}

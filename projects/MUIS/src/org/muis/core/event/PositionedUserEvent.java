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

	public org.muis.core.MuisElementCapture getCapture()
	{
		return theCapture;
	}
}

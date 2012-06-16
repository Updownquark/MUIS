package org.muis.core.event;

import java.awt.Point;

import org.muis.core.MuisElement;

/** An event caused by user interaction for which an x,y point location is relevant */
public class PositionedUserEvent extends UserEvent
{
	private final int theDocumentX;

	private final int theDocumentY;

	private java.util.Map<MuisElement, Point> theElementLocations;

	/**
	 * Creates a positioned user event
	 *
	 * @param type The event type that this event is an instance of
	 * @param doc The document that the mouse event occurred in
	 * @param element The deepest-level element that the event occurred in
	 * @param docX The absolute x-coordinate of the event relative to the document's root element
	 * @param docY The absolute y-coordinate of the event relative to the document's root element
	 */
	public PositionedUserEvent(MuisEventType<Void> type, org.muis.core.MuisDocument doc, MuisElement element, int docX, int docY)
	{
		super(type, doc, element);
		theDocumentX = docX;
		theDocumentY = docY;
		theElementLocations = new java.util.LinkedHashMap<>();
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

	/**
	 * @param element The element
	 * @param p The location of this event over the element
	 */
	public void addElementLocation(MuisElement element, Point p)
	{
		theElementLocations.put(element, p);
	}

	/**
	 * @param element The element
	 * @return The location of this event over the element. This may be null for events that may be but are not always positioned.
	 */
	public Point getPosition(MuisElement element)
	{
		return theElementLocations.get(element);
	}

	/** @return The element-position pairs in this user event, from root to deepest pointed descendant */
	public Iterable<java.util.Map.Entry<MuisElement, Point>> elements()
	{
		return theElementLocations.entrySet();
	}
}

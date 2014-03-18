package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.MuisEventPositionCapture;

/** An event caused by user interaction for which an x,y point location is relevant */
public abstract class PositionedUserEvent extends UserEvent {
	private final MuisEventPositionCapture<?> theCapture;

	/**
	 * Creates a positioned user event
	 *
	 * @param type The event type that this event is an instance of
	 * @param doc The document that the mouse event occurred in
	 * @param element The deepest-level element that the event occurred in
	 * @param capture The capture of the event's location on each element relevant to it
	 */
	public PositionedUserEvent(MuisEventType<Void> type, org.muis.core.MuisDocument doc, MuisElement element,
		MuisEventPositionCapture<?> capture) {
		super(type, doc, element);
		theCapture = capture;
		if(theCapture != null)
			theCapture.seal();
	}

	/** @return The x-coordinate of the event relative target element */
	public int getX() {
		return theCapture.getEventX();
	}

	/** @return The y-coordinate of the event relative to the target element */
	public int getY() {
		return theCapture.getEventY();
	}

	/** @return The capture of the target element */
	public MuisEventPositionCapture<?> getCapture() {
		return theCapture;
	}

	/**
	 * @param element The element to return an event relative to
	 * @return This event relative to the given element. The given element will not be the element returned by {@link #getElement()}, but
	 *         the positions returned by this event's methods will be relative to the given element's position. The event's stateful
	 *         properties ({@link UserEvent#isCanceled() canceled} and {@link UserEvent#isUsed() used}) will be backed by this event's
	 *         properties.
	 */
	public abstract PositionedUserEvent copyFor(MuisElement element);

	/**
	 * @param element The element to get the position of for this event
	 * @return The position of this event over the given element, or null if this information cannot be obtained from this event's capture
	 */
	public java.awt.Point getPosition(MuisElement element) {
		if(theCapture == null)
			return null;
		MuisEventPositionCapture<?> capture = theCapture.find(element);
		if(capture == null)
			for(MuisEventPositionCapture<?> mec : theCapture.iterate(false))
				if(mec.getElement() == element) {
					capture = mec;
					break;
				}
		return capture == null ? null : new java.awt.Point(capture.getEventX(), capture.getEventY());
	}
}

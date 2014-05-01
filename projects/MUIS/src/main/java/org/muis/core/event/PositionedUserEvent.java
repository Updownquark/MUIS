package org.muis.core.event;

import java.util.List;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;
import org.muis.core.MuisEventPositionCapture;

/** An event caused by user interaction for which an x,y point location is relevant */
public abstract class PositionedUserEvent extends UserEvent {
	private final MuisEventPositionCapture<?> theCapture;

	/**
	 * Creates a positioned user event
	 *
	 * @param doc The document that the mouse event occurred in
	 * @param element The element that this event is being fired on
	 * @param target The deepest-level element that the event occurred in
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param time The time at which user performed this action
	 * @param capture The capture of the event's location on each element relevant to it
	 */
	public PositionedUserEvent(MuisDocument doc, MuisElement target, MuisElement element, List<MouseEvent.ButtonType> pressedButtons,
		List<KeyBoardEvent.KeyCode> pressedKeys, long time, MuisEventPositionCapture<?> capture) {
		super(doc, target, element, pressedButtons, pressedKeys, time);
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

package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

/** An event caused by user interaction */
public abstract class UserEvent implements MuisEvent {
	private final MuisDocument theDocument;

	private final MuisElement theTarget;

	private final MuisElement theElement;

	private final long theTime;

	private boolean isUsed;

	/**
	 * Creates a user event
	 *
	 * @param doc The document that the event occurred in
	 * @param target The deepest-level element that the event occurred in
	 * @param element The element that this event is being fired on
	 * @param time The time at which user performed this action
	 */
	public UserEvent(MuisDocument doc, MuisElement target, MuisElement element, long time) {
		theDocument = doc;
		theTarget = element;
		theElement = element;
		theTime = time;
	}

	/** @return The document in which this event occurred */
	public MuisDocument getDocument() {
		return theDocument;
	}

	/** @return The deepest-level element that the event occurred in */
	public MuisElement getTarget() {
		return theTarget;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}

	/** @return The time at which the user performed this action */
	public long getTime() {
		return theTime;
	}

	/** @return Whether this event has been acted upon */
	public boolean isUsed() {
		return isUsed;
	}

	/**
	 * Marks this event as used so that it will not be acted upon by any more elements. This method should be called when a listener knows
	 * what the event was intended for by the user and has the capability to perform the intended action. In this case, the event should not
	 * be acted upon elsewhere because its purpose is complete and any further action would be undesirable.
	 */
	public void use() {
		isUsed = true;
	}

	/**
	 * @param element The element to return an event relative to
	 * @return This event relative to the given element. The given element will not be the element returned by {@link #getElement()}, but
	 *         the positions returned by this event's methods will be relative to the given element's position. The event's stateful
	 *         properties (e.g. {@link UserEvent#isUsed() used}) will be backed by this event's properties.
	 */
	public abstract UserEvent copyFor(MuisElement element);

	@Override
	public boolean isOverridden() {
		return false;
	}
}

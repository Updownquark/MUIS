package org.quick.core.event;

import org.qommons.Causable;
import org.quick.core.QuickElement;

/** Represents the occurrence of an event in a Quick element */
public interface QuickEvent extends Causable {
	/** @return The element that this event is being fired on currently */
	QuickElement getElement();

	/**
	 * @return Whether the state change communicated by this event has been overridden by other state changes such that this event should not
	 *         be fired to any more listeners to avoid propagating misinformation
	 */
	boolean isOverridden();

	/** @return The event that may have caused this event. May be null. */
	@Override
	QuickEvent getCause();
}

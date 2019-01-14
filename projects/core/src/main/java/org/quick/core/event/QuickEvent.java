package org.quick.core.event;

import org.quick.core.QuickElement;

/** Represents the occurrence of an event in a Quick element */
public interface QuickEvent {
	/** @return The element that this event is being fired on currently */
	QuickElement getElement();
}

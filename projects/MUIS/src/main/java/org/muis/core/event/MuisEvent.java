package org.muis.core.event;

import org.muis.core.MuisElement;

/** Represents the occurrence of an event in a MUIS element */
public interface MuisEvent {
	/** @return The element that this event is being fired on currently */
	MuisElement getElement();
}

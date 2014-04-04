package org.muis.core.event;

import org.muis.core.event.boole.TypedPredicate;

/**
 * Implementations of this interface provide advanced ways for programmers to filter {@link MuisEvent}s for listening.
 * 
 * @param <E> The type of event this conditional is for
 */
public interface MuisEventCondition<E extends MuisEvent> {
	/** @return The tester that does the actual filtering of the events */
	TypedPredicate<MuisEvent, E> getTester();
}

package org.muis.core.event;

/**
 * Implementations of this interface provide advanced ways for programmers to filter {@link MuisEvent}s for listening.
 *
 * @param <E> The type of event this conditional is for
 */
public interface MuisEventCondition<E extends MuisEvent> extends java.util.function.Function<MuisEvent, E> {
}

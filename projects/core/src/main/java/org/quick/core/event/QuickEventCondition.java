package org.quick.core.event;

/**
 * Implementations of this interface provide advanced ways for programmers to filter {@link QuickEvent}s for listening.
 *
 * @param <E> The type of event this conditional is for
 */
public interface QuickEventCondition<E extends QuickEvent> extends java.util.function.Function<QuickEvent, E> {
}

package org.quick.widget.core.event;

/**
 * Implementations of this interface provide advanced ways for programmers to filter {@link QuickWidgetEvent}s for listening.
 *
 * @param <E> The type of event this conditional is for
 */
public interface QuickEventCondition<E extends QuickWidgetEvent> extends java.util.function.Function<QuickWidgetEvent, E> {
}

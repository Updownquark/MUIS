package org.muis.core.mgr;

import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventCondition;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.boole.TypedPredicate;

/** Represents an event manager that can be listened to */
public interface EventListenerManager {
	/**
	 * @param condition The condition to filter events for
	 * @param listeners The listeners on which to fire events that match the condition
	 */
	public <T extends MuisEvent> void listen(MuisEventCondition<T> condition, MuisEventListener<T>... listeners);

	/**
	 * @param condition The event filter
	 * @param listeners The listeners on which to stop firing events that match the condition
	 */
	public <T extends MuisEvent> void remove(MuisEventCondition<T> condition, MuisEventListener<T>... listeners);

	/**
	 * @param condition The condition to filter events for
	 * @param listeners The listeners on which to fire events that match the condition
	 */
	public <T extends MuisEvent> void listen(TypedPredicate<MuisEvent, T> condition, MuisEventListener<T>... listeners);

	/**
	 * @param condition The event filter
	 * @param listeners The listeners on which to stop firing events that match the condition
	 */
	public <T extends MuisEvent> void remove(TypedPredicate<MuisEvent, T> condition, MuisEventListener<T>... listeners);
}

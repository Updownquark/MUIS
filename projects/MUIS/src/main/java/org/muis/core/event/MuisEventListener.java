package org.muis.core.event;

/**
 * Listens for event on a MUIS element or subtree
 * 
 * @param <E> The type of event to listen for
 */
public interface MuisEventListener<E extends MuisEvent> {
	/**
	 * Called when the event occurs on an element or subtree for which this listener has been registered
	 *
	 * @param event The event that occurred
	 */
	void eventOccurred(E event);
}

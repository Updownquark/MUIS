package org.wam.core.event;

/**
 * Listens for event on a WAM element or subtree
 * 
 * @param <T> The type of property communicated by the element--may be {@link Void} for non-property
 *            event types
 */
public interface WamEventListener<T>
{
	/**
	 * Called when the event occurs on an element or subtree for which this listener has been
	 * registered
	 * 
	 * @param event The event that occurred
	 * @param element The element that this listener was registered for where the event occurred
	 */
	void eventOccurred(WamEvent<? extends T> event, org.wam.core.WamElement element);

	/**
	 * @return Whether the listener listens for events on a particular element or also on its
	 *         descendants
	 */
	boolean isLocal();
}

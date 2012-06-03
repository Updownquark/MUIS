package org.wam.core.event;

/**
 * Represents the occurrence of an event in a WAM element
 * 
 * @param <T> The type of property communicated by this event. Maybe {@link Void} for non-property
 *            event types
 */
public class WamEvent<T>
{
	private final WamEventType<? super T> theType;

	private final T theValue;

	/**
	 * Creates a WAM event
	 * 
	 * @param type The event type that this event is an occurrence of
	 * @param value The property value that is communicated by this event
	 */
	public WamEvent(WamEventType<? super T> type, T value)
	{
		theType = type;
		theValue = value;
	}

	/**
	 * @return The event type that this event is an occurrence of
	 */
	public WamEventType<? super T> getType()
	{
		return theType;
	}

	/**
	 * @return The property value that is communicated by this event
	 */
	public T getValue()
	{
		return theValue;
	}
}

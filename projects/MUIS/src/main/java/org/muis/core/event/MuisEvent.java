package org.muis.core.event;

/**
 * Represents the occurrence of an event in a MUIS element
 * 
 * @param <T> The type of property communicated by this event. Maybe {@link Void} for non-property
 *            event types
 */
public class MuisEvent<T>
{
	private final MuisEventType<? super T> theType;

	private final T theValue;

	/**
	 * Creates a MUIS event
	 * 
	 * @param type The event type that this event is an occurrence of
	 * @param value The property value that is communicated by this event
	 */
	public MuisEvent(MuisEventType<? super T> type, T value)
	{
		theType = type;
		theValue = value;
	}

	/**
	 * @return The event type that this event is an occurrence of
	 */
	public MuisEventType<? super T> getType()
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

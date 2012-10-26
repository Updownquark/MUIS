package org.muis.core.event;

/**
 * Represents an event that occurs when a property changes value. This event contains the value of the property before the change occurred.
 * This event is fired <b>after</b> the property changes, so that the event's {@link #getValue() value} is the current value of the
 * property.
 *
 * @param <T> The type of the property that was changed
 */
public class MuisPropertyEvent<T> extends MuisEvent<T>
{
	private final T theOldValue;

	/**
	 * @param type The type of the event
	 * @param oldValue The value of the property before it was changed
	 * @param newValue The current value of the property after it has been changed
	 */
	public MuisPropertyEvent(MuisEventType<T> type, T oldValue, T newValue)
	{
		super(type, newValue);
		theOldValue = oldValue;
	}

	/** @return The value of the property before it was changed */
	public T getOldValue()
	{
		return theOldValue;
	}
}

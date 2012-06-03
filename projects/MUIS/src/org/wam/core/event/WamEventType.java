package org.wam.core.event;

/**
 * A type of event that may occur on an element in a WAM document
 * 
 * @param <T> The type of property that this event represents--may be {@link Void} for non-property
 *            events
 */
public class WamEventType<T>
{
	private final String theName;

	private final Class<? extends T> thePropertyType;

	/**
	 * Creates an event type
	 * 
	 * @param name the name of the event
	 * @param propType The type of the event (may be null for non-property events)
	 */
	public WamEventType(String name, Class<? extends T> propType)
	{
		theName = name;
		thePropertyType = propType;
	}

	/**
	 * @return The name of the event
	 */
	public String getName()
	{
		return theName;
	}

	/**
	 * @return The property type for the event
	 */
	public Class<? extends T> getPropertyType()
	{
		return thePropertyType;
	}

	@Override
	public final boolean equals(Object o)
	{
		return super.equals(o);
	}

	@Override
	public final int hashCode()
	{
		return super.hashCode();
	}
}

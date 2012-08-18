package org.muis.core.event;

import org.muis.core.MuisAttribute;

/**
 * Fired when a new value is set on an attribute
 *
 * @param <T> The type of the attribute
 */
public class AttributeChangedEvent<T> extends MuisPropertyEvent<T>
{
	private final MuisAttribute<T> theAttr;

	/**
	 * @param attr The attribute whose value changed
	 * @param oldValue The attribute's value before it was changed
	 * @param newValue The attribute's value after it was changed
	 */
	public AttributeChangedEvent(MuisAttribute<T> attr, T oldValue, T newValue)
	{
		super((MuisEventType<T>) org.muis.core.MuisConstants.Events.ATTRIBUTE_CHANGED, oldValue, newValue);
		theAttr = attr;
	}

	/** @return The attribute whose value changed */
	public MuisAttribute<T> getAttribute()
	{
		return theAttr;
	}
}

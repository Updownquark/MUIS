package org.muis.style;

/**
 * Represents the change to a single style attribute in a style
 * 
 * @param <T> The type of attribute that was changed
 */
public class StyleAttributeEvent<T> extends org.muis.core.event.MuisEvent<Void>
{
	/** The event type that this event is */
	public static final org.muis.core.event.MuisEventType<Void> TYPE = new org.muis.core.event.MuisEventType<Void>(
		"Style Attribute Event", null);

	private final MuisStyle theStyle;

	private final StyleAttribute<T> theAttribute;

	private final T theValue;

	/**
	 * Creates a style attribute event
	 * 
	 * @param style The style that the change was in
	 * @param attr The attribute whose value was changed
	 * @param value The new value of the attribute in the style
	 */
	public StyleAttributeEvent(MuisStyle style, StyleAttribute<T> attr, T value)
	{
		super(TYPE, null);
		theStyle = style;
		theAttribute = attr;
		theValue = value;
	}

	/**
	 * @return The style that the attribute was changed in
	 */
	public MuisStyle getStyle()
	{
		return theStyle;
	}

	/**
	 * @return The attribute whose value was changed
	 */
	public StyleAttribute<T> getAttribute()
	{
		return theAttribute;
	}

	/**
	 * @return The new value of the attribute in the style
	 */
	public T getNewValue()
	{
		return theValue;
	}
}

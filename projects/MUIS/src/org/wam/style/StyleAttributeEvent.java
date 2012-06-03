package org.wam.style;

/**
 * Represents the change to a single style attribute in a style
 * 
 * @param <T> The type of attribute that was changed
 */
public class StyleAttributeEvent<T> extends org.wam.core.event.WamEvent<Void>
{
	/** The event type that this event is */
	public static final org.wam.core.event.WamEventType<Void> TYPE = new org.wam.core.event.WamEventType<Void>(
		"Style Attribute Event", null);

	private final WamStyle theStyle;

	private final StyleAttribute<T> theAttribute;

	private final T theValue;

	/**
	 * Creates a style attribute event
	 * 
	 * @param style The style that the change was in
	 * @param attr The attribute whose value was changed
	 * @param value The new value of the attribute in the style
	 */
	public StyleAttributeEvent(WamStyle style, StyleAttribute<T> attr, T value)
	{
		super(TYPE, null);
		theStyle = style;
		theAttribute = attr;
		theValue = value;
	}

	/**
	 * @return The style that the attribute was changed in
	 */
	public WamStyle getStyle()
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

package org.muis.core.style;

/**
 * Represents the change to a single style attribute in a style
 *
 * @param <T> The type of attribute that was changed
 */
public class StyleAttributeEvent<T> extends org.muis.core.event.MuisEvent<Void> {
	/** The event type that this event is */
	public static final org.muis.core.event.MuisEventType<Void> TYPE = new org.muis.core.event.MuisEventType<>("Style Attribute Event",
		null);

	private MuisStyle theRootStyle;

	private MuisStyle theLocalStyle;

	private final StyleAttribute<T> theAttribute;

	private final T theValue;

	/**
	 * Creates a style attribute event
	 *
	 * @param root The style that the change was in
	 * @param local The style that is firing the event
	 * @param attr The attribute whose value was changed
	 * @param value The new value of the attribute in the style
	 */
	public StyleAttributeEvent(MuisStyle root, MuisStyle local, StyleAttribute<T> attr, T value) {
		super(TYPE, null);
		theRootStyle = root;
		theLocalStyle = local;
		theAttribute = attr;
		theValue = value;
	}

	/** @return The style that the attribute was changed in */
	public MuisStyle getRootStyle() {
		return theRootStyle;
	}

	/** @return The style that is firing the event */
	public MuisStyle getLocalStyle() {
		return theLocalStyle;
	}

	/** @return The attribute whose value was changed */
	public StyleAttribute<T> getAttribute() {
		return theAttribute;
	}

	/** @return The new value of the attribute in the style */
	public T getNewValue() {
		return theValue;
	}
}

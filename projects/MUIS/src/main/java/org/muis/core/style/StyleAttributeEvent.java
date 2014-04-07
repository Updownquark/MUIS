package org.muis.core.style;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.boole.TypedPredicate;

/**
 * Represents the change to a single style attribute in a style
 *
 * @param <T> The type of attribute that was changed
 */
public class StyleAttributeEvent<T> implements org.muis.core.event.MuisEvent {
	/** Filters events of this type */
	public static final TypedPredicate<MuisEvent, StyleAttributeEvent<?>> base = new TypedPredicate<MuisEvent, StyleAttributeEvent<?>>() {
		@Override
		public StyleAttributeEvent<?> cast(MuisEvent value) {
			return value instanceof StyleAttributeEvent ? (StyleAttributeEvent<?>) value : null;
		}
	};

	/**
	 * A filter for style attribute events on a particular attribute
	 *
	 * @param <T> The type of the attribute
	 */
	public static class StyleAttributeTypedPredicate<T> implements TypedPredicate<StyleAttributeEvent<?>, StyleAttributeEvent<T>> {
		private final StyleAttribute<T> theAttribute;

		private StyleAttributeTypedPredicate(StyleAttribute<T> att) {
			theAttribute = att;
		}

		/** @return The attribute that this filter accepts events for */
		public StyleAttribute<T> getAttribute() {
			return theAttribute;
		}

		@Override
		public StyleAttributeEvent<T> cast(StyleAttributeEvent<?> value) {
			if(value.getAttribute() == theAttribute)
				return (StyleAttributeEvent<T>) value;
			else
				return null;
		}
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to listen for
	 * @return A filter for change events to the given attribute
	 */
	public static <T> TypedPredicate<MuisEvent, StyleAttributeEvent<T>> style(StyleAttribute<T> attr) {
		return new org.muis.core.event.boole.TPAnd<>(base, new StyleAttributeTypedPredicate<>(attr));
	}

	private final MuisElement theElement;

	private MuisStyle theRootStyle;

	private MuisStyle theLocalStyle;

	private final StyleAttribute<T> theAttribute;

	private final T theValue;

	/**
	 * Creates a style attribute event
	 *
	 * @param element The element that this event is for--may be null
	 * @param root The style that the change was in
	 * @param local The style that is firing the event
	 * @param attr The attribute whose value was changed
	 * @param value The new value of the attribute in the style
	 */
	public StyleAttributeEvent(MuisElement element, MuisStyle root, MuisStyle local, StyleAttribute<T> attr, T value) {
		theElement = element;
		theRootStyle = root;
		theLocalStyle = local;
		theAttribute = attr;
		theValue = value;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
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

package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;

/**
 * Represents an event that occurs when a property changes value.
 *
 * @param <T> The type of the property that was changed
 */
public class MuisPropertyEvent<T> implements MuisEvent {
	/** Filters for property events */
	public static final TypedPredicate<MuisEvent, MuisPropertyEvent<?>> base = new TypedPredicate<MuisEvent, MuisPropertyEvent<?>>() {
		@Override
		public MuisPropertyEvent<?> cast(MuisEvent value) {
			return value instanceof MuisPropertyEvent ? (MuisPropertyEvent<?>) value : null;
		}
	};

	private final MuisElement theElement;
	private final T theOldValue;
	private final T theNewValue;

	/**
	 * @param element The element that this event is being fired in
	 * @param oldValue The value of the property before it was changed
	 * @param newValue The current value of the property after it has been changed
	 */
	public MuisPropertyEvent(MuisElement element, T oldValue, T newValue) {
		theElement = element;
		theOldValue = oldValue;
		theNewValue = newValue;
	}

	/** @return The value of the property before it was changed */
	public T getOldValue() {
		return theOldValue;
	}

	/** @return The new value of the property */
	public T getValue() {
		return theNewValue;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}
}

package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;

/**
 * Represents an event that occurs when a property changes value.
 *
 * @param <T> The type of the property that was changed
 */
public abstract class MuisPropertyEvent<T> implements MuisEvent {
	/** Filters for property events */
	public static final TypedPredicate<MuisEvent, MuisPropertyEvent<?>> base = new TypedPredicate<MuisEvent, MuisPropertyEvent<?>>() {
		@Override
		public MuisPropertyEvent<?> cast(MuisEvent value) {
			return value instanceof MuisPropertyEvent && !((MuisPropertyEvent<?>) value).isOverridden() ? (MuisPropertyEvent<?>) value
				: null;
		}
	};

	private final MuisElement theElement;
	private final T theNewValue;

	/**
	 * @param element The element that this event is being fired in
	 * @param newValue The current value of the property after it has been changed
	 */
	public MuisPropertyEvent(MuisElement element, T newValue) {
		theElement = element;
		theNewValue = newValue;
	}

	/** @return The new value of the property */
	public T getValue() {
		return theNewValue;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}

	/**
	 * A property event is overridden when the value of the property on the same property center is changed again while the event from the
	 * old change is still being fired. Overridden property events should stop being propagated to listeners because listeners using the
	 * {@link #getValue() value} on the event will be using outdated information.
	 *
	 * @return Whether this event is overridden.
	 */
	@Override
	public abstract boolean isOverridden();
}

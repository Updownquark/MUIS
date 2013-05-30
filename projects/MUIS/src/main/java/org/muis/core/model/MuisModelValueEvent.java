package org.muis.core.model;

import org.muis.core.event.MuisEvent;

/**
 * An event representing a user's choice to change the value of {@link MuisModelValue}
 * 
 * @param <T> The type of value changed
 */
public class MuisModelValueEvent<T> {
	private final MuisModelValue<T> theModelValue;

	private final MuisEvent<?> theUserEvent;

	private final T theOldValue;

	private final T theNewValue;

	/**
	 * @param modelValue The model value that was modified
	 * @param userEvent The event that caused the change. May be null.
	 * @param oldValue The value before the change
	 * @param newValue The value after the change
	 */
	public MuisModelValueEvent(MuisModelValue<T> modelValue, MuisEvent<?> userEvent, T oldValue, T newValue) {
		theModelValue = modelValue;
		theUserEvent = userEvent;
		theOldValue = oldValue;
		theNewValue = newValue;
	}

	/** @return The model value that was modified */
	public MuisModelValue<T> getModelValue() {
		return theModelValue;
	}

	/** @return The event that caused the change. May be null. */
	public MuisEvent<?> getUserEvent() {
		return theUserEvent;
	}

	/** @return The value before the change */
	public T getOldValue() {
		return theOldValue;
	}

	/** @return The value after the change */
	public T getNewValue() {
		return theNewValue;
	}
}

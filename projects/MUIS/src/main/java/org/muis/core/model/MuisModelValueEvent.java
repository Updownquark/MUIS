package org.muis.core.model;

import org.muis.core.event.MuisEvent;

public class MuisModelValueEvent<T> {
	private final MuisModelValue<T> theModelValue;

	private final MuisEvent<?> theUserEvent;

	private final T theOldValue;

	private final T theNewValue;

	public MuisModelValueEvent(MuisModelValue<T> modelValue, MuisEvent<?> userEvent, T oldValue, T newValue) {
		theModelValue = modelValue;
		theUserEvent = userEvent;
		theOldValue = oldValue;
		theNewValue = newValue;
	}

	public MuisModelValue<T> getTheModelValue() {
		return theModelValue;
	}

	public MuisEvent<?> getTheUserEvent() {
		return theUserEvent;
	}

	public T getTheOldValue() {
		return theOldValue;
	}

	public T getTheNewValue() {
		return theNewValue;
	}
}

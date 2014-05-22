package org.muis.core.model;

import org.muis.core.event.UserEvent;
import org.muis.core.rx.ObservableValueEvent;

/**
 * An event representing a user's choice to change the value of {@link MuisModelValue}
 *
 * @param <T> The type of value changed
 */
public class MuisModelValueEvent<T> extends ObservableValueEvent<T> {
	private final UserEvent theUserEvent;

	/**
	 * @param modelValue The model value that was modified
	 * @param userEvent The event that caused the change. May be null.
	 * @param oldValue The value before the change
	 * @param newValue The value after the change
	 */
	public MuisModelValueEvent(MuisModelValue<T> modelValue, UserEvent userEvent, T oldValue, T newValue) {
		super(modelValue, oldValue, newValue, null);
		theUserEvent = userEvent;
	}

	/** @return The model value that was modified */
	@Override
	public MuisModelValue<T> getObservable() {
		return (MuisModelValue<T>) super.getObservable();
	}

	/** @return The event that caused the change. May be null. */
	public UserEvent getUserEvent() {
		return theUserEvent;
	}

	/**
	 * @param event The observable event
	 * @return The user event that ultimately caused the observable event (may be null)
	 */
	public static UserEvent getUserEvent(ObservableValueEvent<?> event) {
		while(event != null && !(event instanceof MuisModelValueEvent))
			event = event.getCause();
		if(event != null)
			return ((MuisModelValueEvent<?>) event).getUserEvent();
		else
			return null;
	}
}

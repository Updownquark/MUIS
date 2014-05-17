package org.muis.core.model;

import org.muis.core.rx.ObservableValue;

/**
 * Represents a single, simple value that can be changed by the user
 *
 * @param <T> The (compile-time) type of the value
 */
public interface MuisModelValue<T> extends ObservableValue<T> {
	/**
	 * @param value The value to set
	 * @param event The user event that caused the change. May be null.
	 */
	void set(T value, org.muis.core.event.UserEvent event);
}

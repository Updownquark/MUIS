package org.muis.core.rx;

/**
 * A listener to be notified when an observable changes
 * @param <T> The super type of observables that this listener can listen to
 */
@FunctionalInterface
public interface ObservableValueListener<T> {
	/** @param event The event representing the value change */
	void valueChanged(ObservableValueEvent<? extends T> event);
}

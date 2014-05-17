package org.muis.core.rx;

/**
 * An event representing the change of an observable's value
 * 
 * @param <T> The compile-time type of the observable's value
 */
public class ObservableEvent<T> {
	private final ObservableValue<T> theObservable;

	private final T theOldValue;
	private final T theNewValue;
	private final ObservableEvent<?> theCause;

	/**
	 * @param observable The observable whose value changed
	 * @param oldValue The old value of the observable
	 * @param newValue The new value in the observable
	 * @param cause The observable event that caused this event (may be null)
	 */
	public ObservableEvent(ObservableValue<T> observable, T oldValue, T newValue, ObservableEvent<?> cause) {
		theObservable = observable;
		theOldValue = oldValue;
		theNewValue = newValue;
		theCause = cause;
	}

	/** @return The observable that caused this event */
	public ObservableValue<T> getObservable() {
		return theObservable;
	}

	/** @return The old value of the observable */
	public T getOldValue() {
		return theOldValue;
	}

	/** @return The new value in the observable */
	public T getNewValue() {
		return theNewValue;
	}

	/** @return The observable that caused this event (may be null) */
	public ObservableEvent<?> getCause() {
		return theCause;
	}
}

package org.muis.core.rx;

public class ObservableEvent<T> {
	private final Observable<T> theObservable;

	private final T theOldValue;
	private final T theNewValue;
	private final ObservableEvent<?> theCause;

	public ObservableEvent(Observable<T> observable, T oldValue, T newValue, ObservableEvent<?> cause) {
		theObservable = observable;
		theOldValue = oldValue;
		theNewValue = newValue;
		theCause = cause;
	}

	public Observable<T> getObservable() {
		return theObservable;
	}

	public T getOldValue() {
		return theOldValue;
	}

	public T getNewValue() {
		return theNewValue;
	}

	public ObservableEvent<?> getCause() {
		return theCause;
	}
}

package org.muis.util;

import org.muis.rx.Observable;
import org.muis.rx.Observer;

/**
 * Wraps an observable
 * 
 * @param <T> The type of the observable
 */
public class ObservableWrapper<T> implements Observable<T> {
	private final Observable<T> theWrapped;

	/** @param wrap The observable to wrap */
	public ObservableWrapper(Observable<T> wrap) {
		theWrapped = wrap;
	}

	@Override
	public Runnable internalSubscribe(Observer<? super T> observer) {
		return theWrapped.internalSubscribe(observer);
	}

	@Override
	public String toString() {
		return theWrapped.toString();
	}
}

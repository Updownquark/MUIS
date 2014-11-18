package org.muis.util;

import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.ObservableValueEvent;
import org.muis.core.rx.Observer;

import prisms.lang.Type;

/**
 * An observable value that wraps another
 *
 * @param <T> The type of the value
 */
public class ObservableValueWrapper<T> implements ObservableValue<T> {
	private final ObservableValue<T> theWrapped;

	/** @param wrap The value to wrap */
	public ObservableValueWrapper(ObservableValue<T> wrap) {
		theWrapped = wrap;
	}

	@Override
	public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
		return theWrapped.internalSubscribe(observer);
	}

	@Override
	public Type getType() {
		return theWrapped.getType();
	}

	@Override
	public T get() {
		return theWrapped.get();
	}

	@Override
	public String toString() {
		return theWrapped.toString();
	}
}

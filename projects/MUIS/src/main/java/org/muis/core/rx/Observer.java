package org.muis.core.rx;

/**
 * Listens to an observable
 * 
 * @param <T> The super type of observable that this observer may listen to
 */
public interface Observer<T> {
	/** @param value The latest value on the observable */
	<V extends T> void onNext(V value);

	/** Signals that the observable has no more values */
	default void onCompleted() {
	}

	/** @param e The error that occured in the observable */
	default void onError(Throwable e) {
	}
}

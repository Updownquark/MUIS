package org.muis.core.rx;

public interface Observer<T> {
	<V extends T> void onNext(V value);

	default void onCompleted() {
	}

	default void onError(Throwable e) {
	}
}

package org.muis.core.rx;

public interface Observer<T> {
	void onNext(T value);

	default void onCompleted() {
	}

	default void onError(Throwable e) {
	}
}

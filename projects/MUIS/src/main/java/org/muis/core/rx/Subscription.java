package org.muis.core.rx;

public interface Subscription<T> extends Observable<T> {
	void unsubscribe();
}

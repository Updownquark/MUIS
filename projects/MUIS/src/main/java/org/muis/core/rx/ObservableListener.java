package org.muis.core.rx;

public interface ObservableListener<T> {
	void eventOccurred(ObservableEvent<? extends T> event);
}

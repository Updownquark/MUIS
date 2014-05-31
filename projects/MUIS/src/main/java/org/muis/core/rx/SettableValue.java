package org.muis.core.rx;

public interface SettableValue<T> extends ObservableValue<T> {
	<V extends T> SettableValue<T> set(V value) throws IllegalArgumentException;
}

package org.muis.core.rx;


public interface Observable<T> {
	Class<T> getType();
	T get();

	Observable<T> addListener(ObservableListener<? super T> listener);

	Observable<T> removeListener(ObservableListener<?> listener);
}

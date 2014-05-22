package org.muis.core.rx;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Observable<T> {
	Subscription<T> subscribe(Observer<? super T> observer);

	default <R> Observable<R> map(Function<T, R> func) {
		return new ComposedObservable<>(args -> {
			return func.apply((T) args[0]);
		}, this);
	};

	default <V, R> Observable<R> combine(Observable<V> other, BiFunction<T, V, R> func) {
		return new ComposedObservable<>(args -> {
			return func.apply((T) args[0], (V) args[1]);
		}, this, other);
	}
}

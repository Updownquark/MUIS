package org.muis.core.rx;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A value holder that can notify listeners when the value changes
 *
 * @param <T> The compile-time type of this observable's value
 */
public interface Observable<T> {
	/** @return The type of value this observable contains. May be null if this observable's value is always null. */
	Type getType();

	/** @return The current value of this observable */
	T get();

	/**
	 * @param listener The listener to be notified when this observable's value changes
	 * @return This observable, for chaining
	 */
	Observable<T> addListener(ObservableListener<? super T> listener);

	/**
	 * @param listener The listener to stop notifying when this observable's value changes
	 * @return This observable, for chaining
	 */
	Observable<T> removeListener(ObservableListener<?> listener);

	/**
	 * Composes this observable into another observable that depends on this one
	 *
	 * @param <R> The type of the new observable
	 * @param function The function to apply to this observable's value
	 * @return The new observable whose value is a function of this observable's value
	 */
	default <R> Observable<R> compose(Function<T, R> function) {
		return new ComposedObservable<>((Object [] args) -> {
			return function.apply((T) args[0]);
		}, this);
	};

	/**
	 * Composes this observable into another observable that depends on this one and one other
	 *
	 * @param <U> The type of the other argument observable
	 * @param <R> The type of the new observable
	 * @param function The function to apply to the values of the observables
	 * @param arg The other observable to be composed
	 * @return The new observable whose value is a function of this observable's value and the other's
	 */
	default <U, R> Observable<R> compose(BiFunction<T, U, R> function, Observable<U> arg) {
		return new ComposedObservable<>((Object [] args) -> {
			return function.apply((T) args[0], (U) args[1]);
		}, this, arg);
	}

	/**
	 * Composes this observable into another observable that depends on this one and two others
	 *
	 * @param <U> The type of the first other argument observable
	 * @param <V> The type of the second other argument observable
	 * @param <R> The type of the new observable
	 * @param function The function to apply to the values of the observables
	 * @param arg2 The first other observable to be composed
	 * @param arg3 The second other observable to be composed
	 * @return The new observable whose value is a function of this observable's value and the others'
	 */
	default <U, V, R> Observable<R> compose(TriFunction<T, U, V, R> function, Observable<U> arg2, Observable<V> arg3) {
		return new ComposedObservable<>((Object [] args) -> {
			return function.apply((T) args[0], (U) args[1], (V) args[3]);
		}, this, arg2, arg3);
	}

	/**
	 * @param <X> The type of the value to wrap
	 * @param value The value to wrap
	 * @return An observable that always returns the given value
	 */
	public static <X> Observable<X> constant(final X value) {
		return new Observable<X>() {
			@Override
			public Class<? extends X> getType() {
				return value == null ? null : (Class<? extends X>) value.getClass();
			}

			@Override
			public X get() {
				return value;
			}

			@Override
			public Observable<X> addListener(ObservableListener<? super X> listener) {
				return this; // Immutable--no need to store the listeners
			}

			@Override
			public Observable<X> removeListener(ObservableListener<?> listener) {
				return this;
			}
		};
	}
}

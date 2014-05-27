package org.muis.core.rx;

import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A value holder that can notify listeners when the value changes
 *
 * @param <T> The compile-time type of this observable's value
 */
public interface ObservableValue<T> extends Observable<ObservableValueEvent<T>> {
	/** @return The type of value this observable contains. May be null if this observable's value is always null. */
	Type getType();

	/** @return The current value of this observable */
	T get();

	/**
	 * Composes this observable into another observable that depends on this one
	 *
	 * @param <R> The type of the new observable
	 * @param function The function to apply to this observable's value
	 * @return The new observable whose value is a function of this observable's value
	 */
	default <R> ObservableValue<R> compose(Function<T, R> function) {
		return new ComposedObservableValue<>((Object [] args) -> {
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
	default <U, R> ObservableValue<R> compose(BiFunction<T, U, R> function, ObservableValue<U> arg) {
		return new ComposedObservableValue<>((Object [] args) -> {
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
	default <U, V, R> ObservableValue<R> compose(TriFunction<T, U, V, R> function, ObservableValue<U> arg2, ObservableValue<V> arg3) {
		return new ComposedObservableValue<>((Object [] args) -> {
			return function.apply((T) args[0], (U) args[1], (V) args[3]);
		}, this, arg2, arg3);
	}

	/**
	 * @param <X> The type of the value to wrap
	 * @param value The value to wrap
	 * @return An observable that always returns the given value
	 */
	public static <X> ObservableValue<X> constant(final X value) {
		return new ObservableValue<X>() {
			@Override
			public Type getType() {
				return value == null ? null : new Type(value.getClass());
			}

			@Override
			public X get() {
				return value;
			}

			@Override
			public Subscription<ObservableValueEvent<X>> subscribe(Observer<? super ObservableValueEvent<X>> observer) {
				return new Subscription<ObservableValueEvent<X>>() {
					@Override
					public Subscription<ObservableValueEvent<X>> subscribe(Observer<? super ObservableValueEvent<X>> observer2) {
						return this;
					}

					@Override
					public void unsubscribe() {
					}
				};
			}
		};
	}
}

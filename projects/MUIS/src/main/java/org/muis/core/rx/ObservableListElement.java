package org.muis.core.rx;

import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * An observable element that knows its position in the list
 *
 * @param <T> The type of the element
 */
public interface ObservableListElement<T> extends ObservableElement<T> {
	/** @return The index of this element within its list */
	int getIndex();

	/**
	 * Composes this observable into another observable that depends on this one
	 *
	 * @param <R> The type of the new observable
	 * @param function The function to apply to this observable's value
	 * @return The new observable whose value is a function of this observable's value
	 */
	@Override
	default <R> ObservableListElement<R> mapV(Function<? super T, R> function) {
		return mapV(null, function, false);
	};

	/**
	 * Composes this observable into another observable that depends on this one
	 *
	 * @param <R> The type of the new observable
	 * @param type The run-time type of the new observable
	 * @param function The function to apply to this observable's value
	 * @return The new observable whose value is a function of this observable's value
	 */
	@Override
	default <R> ObservableListElement<R> mapV(Type type, Function<? super T, R> function, boolean combineNull) {
		return new ComposedObservableListElement<>(this, type, args -> {
			return function.apply((T) args[0]);
		}, combineNull, this);
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
	@Override
	default <U, R> ObservableListElement<R> combineV(BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg) {
		return combineV(null, function, arg, false);
	}

	/**
	 * Composes this observable into another observable that depends on this one and one other
	 *
	 * @param <U> The type of the other argument observable
	 * @param <R> The type of the new observable
	 * @param type The run-time type of the new observable
	 * @param function The function to apply to the values of the observables
	 * @param arg The other observable to be composed
	 * @return The new observable whose value is a function of this observable's value and the other's
	 */
	@Override
	default <U, R> ObservableListElement<R> combineV(Type type, BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg,
		boolean combineNull) {
		return new ComposedObservableListElement<>(this, type, args -> {
			return function.apply((T) args[0], (U) args[1]);
		}, combineNull, this, arg);
	}

	/**
	 * @param <U> The type of the other observable to tuplize
	 * @param arg The other observable to tuplize
	 * @return An observable which broadcasts tuples of the latest values of this observable value and another
	 */
	@Override
	default <U> ObservableListElement<BiTuple<T, U>> tupleV(ObservableValue<U> arg) {
		return combineV(BiTuple<T, U>::new, arg);
	}

	/**
	 * @param <U> The type of the first other observable to tuplize
	 * @param <V> The type of the second other observable to tuplize
	 * @param arg1 The first other observable to tuplize
	 * @param arg2 The second other observable to tuplize
	 * @return An observable which broadcasts tuples of the latest values of this observable value and 2 others
	 */
	@Override
	default <U, V> ObservableListElement<TriTuple<T, U, V>> tupleV(ObservableValue<U> arg1, ObservableValue<V> arg2) {
		return combineV(TriTuple<T, U, V>::new, arg1, arg2);
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
	@Override
	default <U, V, R> ObservableListElement<R> combineV(TriFunction<? super T, ? super U, ? super V, R> function, ObservableValue<U> arg2,
		ObservableValue<V> arg3) {
		return combineV(null, function, arg2, arg3, false);
	}

	/**
	 * Composes this observable into another observable that depends on this one and two others
	 *
	 * @param <U> The type of the first other argument observable
	 * @param <V> The type of the second other argument observable
	 * @param <R> The type of the new observable
	 * @param type The run-time type of the new observable
	 * @param function The function to apply to the values of the observables
	 * @param arg2 The first other observable to be composed
	 * @param arg3 The second other observable to be composed
	 * @return The new observable whose value is a function of this observable's value and the others'
	 */
	@Override
	default <U, V, R> ObservableListElement<R> combineV(Type type, TriFunction<? super T, ? super U, ? super V, R> function,
		ObservableValue<U> arg2, ObservableValue<V> arg3, boolean combineNull) {
		return new ComposedObservableListElement<>(this, type, args -> {
			return function.apply((T) args[0], (U) args[1], (V) args[2]);
		}, combineNull, this, arg2, arg3);
	}

	/** @param <T> The type of the element */
	class ComposedObservableListElement<T> extends ComposedObservableValue<T> implements ObservableListElement<T> {
		private final ObservableListElement<?> theRoot;

		public ComposedObservableListElement(ObservableListElement<?> root, Type t, Function<Object [], T> f, boolean combineNull,
			ObservableValue<?>... composed) {
			super(t, f, combineNull, composed);
			theRoot = root;
		}

		@Override
		public ObservableValue<T> persistent() {
			ObservableValue<?> [] composed = getComposed();
			composed[0] = theRoot.persistent();
			return new ComposedObservableValue<>(getType(), getFunction(), isNullCombined(), composed);
		}

		@Override
		public int getIndex() {
			return theRoot.getIndex();
		}
	}
}
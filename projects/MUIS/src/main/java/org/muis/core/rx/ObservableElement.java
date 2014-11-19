package org.muis.core.rx;

import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * An observable wrapper around an element in a {@link ObservableCollection}. This observable will call its observers'
 * {@link Observer#onCompleted(Object)} method when the element is removed from the collection. This element will also complete when the
 * subscription used to subscribe to the outer collection is {@link Subscription#unsubscribe() unsubscribed}.
 *
 * @param <T> The type of the element
 */
public interface ObservableElement<T> extends ObservableValue<T> {
	/** @return An observable value that keeps reporting updates after the subscription to the parent collection is unsubscribed */
	ObservableValue<T> persistent();

	@Override
	default <R> ObservableElement<R> mapV(Function<? super T, R> function) {
		return mapV(null, function, false);
	};

	@Override
	default <R> ObservableElement<R> mapV(Type type, Function<? super T, R> function, boolean combineNull) {
		return new ComposedObservableElement<>(this, type, args -> {
			return function.apply((T) args[0]);
		}, combineNull, this);
	};

	@Override
	default <U, R> ObservableElement<R> combineV(BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg) {
		return combineV(null, function, arg, false);
	}

	@Override
	default <U, R> ObservableElement<R> combineV(Type type, BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg,
		boolean combineNull) {
		return new ComposedObservableElement<>(this, type, args -> {
			return function.apply((T) args[0], (U) args[1]);
		}, combineNull, this, arg);
	}

	@Override
	default <U> ObservableElement<BiTuple<T, U>> tupleV(ObservableValue<U> arg) {
		return combineV(BiTuple<T, U>::new, arg);
	}

	@Override
	default <U, V> ObservableElement<TriTuple<T, U, V>> tupleV(ObservableValue<U> arg1, ObservableValue<V> arg2) {
		return combineV(TriTuple<T, U, V>::new, arg1, arg2);
	}

	@Override
	default <U, V, R> ObservableElement<R> combineV(TriFunction<? super T, ? super U, ? super V, R> function, ObservableValue<U> arg2,
		ObservableValue<V> arg3) {
		return combineV(null, function, arg2, arg3, false);
	}

	@Override
	default <U, V, R> ObservableElement<R> combineV(Type type, TriFunction<? super T, ? super U, ? super V, R> function,
		ObservableValue<U> arg2, ObservableValue<V> arg3, boolean combineNull) {
		return new ComposedObservableElement<>(this, type, args -> {
			return function.apply((T) args[0], (U) args[1], (V) args[2]);
		}, combineNull, this, arg2, arg3);
	}

	@Override
	default ObservableElement<T> refireWhen(Observable<?> observable) {
		ObservableElement<T> outer = this;
		return new ObservableElement<T>() {
			@Override
			public Type getType() {
				return outer.getType();
			}

			@Override
			public T get() {
				return outer.get();
			}

			@Override
			public ObservableValue<T> persistent() {
				return outer.persistent().refireWhen(observable);
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
				Runnable outerSub = outer.internalSubscribe(observer);
				Runnable refireSub = observable.internalSubscribe(new Observer<Object>() {
					@Override
					public <V> void onNext(V value) {
						observer.onNext(createEvent(get(), get(), value));
					}
				});
				return () -> {
					outerSub.run();
					refireSub.run();
				};
			}
		};
	}

	/** @param <T> The type of the element */
	class ComposedObservableElement<T> extends ComposedObservableValue<T> implements ObservableElement<T> {
		private final ObservableElement<?> theRoot;

		public ComposedObservableElement(ObservableElement<?> root, Type t, Function<Object [], T> f, boolean combineNull,
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
	}
}

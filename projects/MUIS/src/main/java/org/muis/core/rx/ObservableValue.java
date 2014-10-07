package org.muis.core.rx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A value holder that can notify listeners when the value changes
 *
 * @param <T> The compile-time type of this observable's value
 */
public interface ObservableValue<T> extends Observable<ObservableValueEvent<T>>, java.util.function.Supplier<T> {
	/** @return The type of value this observable contains. May be null if this observable's value is always null. */
	Type getType();

	/** @return The current value of this observable */
	@Override
	T get();

	/** @return An observable that just reports this observable value's value in an observable without the event */
	default Observable<T> value() {
		return map(event -> event.getValue());
	}

	/**
	 * Composes this observable into another observable that depends on this one
	 *
	 * @param <R> The type of the new observable
	 * @param function The function to apply to this observable's value
	 * @return The new observable whose value is a function of this observable's value
	 */
	default <R> ObservableValue<R> mapV(Function<? super T, R> function) {
		return mapV(null, function);
	};

	/**
	 * Composes this observable into another observable that depends on this one
	 *
	 * @param <R> The type of the new observable
	 * @param type The run-time type of the new observable
	 * @param function The function to apply to this observable's value
	 * @return The new observable whose value is a function of this observable's value
	 */
	default <R> ObservableValue<R> mapV(Type type, Function<? super T, R> function) {
		return new ComposedObservableValue<>(type, args -> {
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
	default <U, R> ObservableValue<R> combineV(BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg) {
		return combineV(null, function, arg);
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
	default <U, R> ObservableValue<R> combineV(Type type, BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg) {
		return new ComposedObservableValue<>(type, args -> {
			return function.apply((T) args[0], (U) args[1]);
		}, this, arg);
	}

	/**
	 * @param <U> The type of the other observable to tuplize
	 * @param arg The other observable to tuplize
	 * @return An observable which broadcasts tuples of the latest values of this observable value and another
	 */
	default <U> ObservableValue<BiTuple<T, U>> tupleV(ObservableValue<U> arg) {
		return combineV(BiTuple<T, U>::new, arg);
	}

	/**
	 * @param <U> The type of the first other observable to tuplize
	 * @param <V> The type of the second other observable to tuplize
	 * @param arg1 The first other observable to tuplize
	 * @param arg2 The second other observable to tuplize
	 * @return An observable which broadcasts tuples of the latest values of this observable value and 2 others
	 */
	default <U, V> ObservableValue<TriTuple<T, U, V>> tupleV(ObservableValue<U> arg1, ObservableValue<V> arg2) {
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
	default <U, V, R> ObservableValue<R> combineV(TriFunction<? super T, ? super U, ? super V, R> function, ObservableValue<U> arg2,
		ObservableValue<V> arg3) {
		return combineV(null, function, arg2, arg3);
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
	default <U, V, R> ObservableValue<R> combineV(Type type, TriFunction<? super T, ? super U, ? super V, R> function,
		ObservableValue<U> arg2, ObservableValue<V> arg3) {
		return new ComposedObservableValue<>(type, args -> {
			return function.apply((T) args[0], (U) args[1], (V) args[2]);
		}, this, arg2, arg3);
	}

	@Override
	default ObservableValue<T> takeUntil(Observable<?> until) {
		AtomicBoolean check = new AtomicBoolean(false);
		Observer<ObservableValueEvent<T>> [] controller = new Observer[1];
		Subscription<?> [] untilSub = new Subscription[1];
		untilSub[0] = until.subscribe(new Observer<Object>() {
			@Override
			public <V> void onNext(V value) {
				if(check.getAndSet(true))
					return;
				if(controller[0] != null)
					controller[0].onCompleted(null);
				if(untilSub[0] != null) {
					untilSub[0].unsubscribe();
					untilSub[0] = null;
				}
			}

			@Override
			public void onCompleted(Object value) {
				if(check.getAndSet(true))
					return;
				if(controller[0] != null)
					controller[0].onCompleted(null);
				if(untilSub[0] != null) {
					untilSub[0].unsubscribe();
					untilSub[0] = null;
				}
			}
		});
		if(check.get() && untilSub[0] != null) {
			untilSub[0].unsubscribe();
			untilSub[0] = null;
		}

		ObservableValue<T> outer = this;
		DefaultObservableValue<T> ret = new DefaultObservableValue<T>() {
			@Override
			public Type getType() {
				return outer.getType();
			}

			@Override
			public T get() {
				return outer.get();
			}
		};
		controller[0] = ret.control(subscriber -> {
			if(check.get()) {
				subscriber.onCompleted(null);
				controller[0].onCompleted(null);
				return;
			}
			Subscription<?> [] sub = new Subscription[1];
			sub[0] = subscribe(new Observer<ObservableValueEvent<T>>() {
				@Override
				public <V extends ObservableValueEvent<T>> void onNext(V value) {
					controller[0].onNext(value);
				}

				@Override
				public <V extends ObservableValueEvent<T>> void onCompleted(V value) {
					controller[0].onCompleted(value);
					if(untilSub[0] != null) {
						untilSub[0].unsubscribe();
						untilSub[0] = null;
					}
				}

				@Override
				public void onError(Throwable e) {
					controller[0].onError(e);
				}
			});
		});
		return ret;
	}

	/**
	 * @param <X> The type of the value to wrap
	 * @param value The value to wrap
	 * @return An observable that always returns the given value
	 */
	public static <X> ObservableValue<X> constant(final X value) {
		return new ConstantObservableValue<>(value == null ? Type.NULL : new Type(value.getClass()), value);
	}

	/**
	 * @param <X> The compile-time type of the value to wrap
	 * @param type The run-time type of the value to wrap
	 * @param value The value to wrap
	 * @return An observable that always returns the given value
	 */
	public static <X> ObservableValue<X> constant(final Type type, final X value) {
		return new ConstantObservableValue<>(type, value);
	}

	/**
	 * @param <T> The compile-time super type of all observables contained in the nested observable
	 * @param type The super type of all observables possibly contained in the given nested observable, or null to use the type of the
	 *            contained observable
	 * @param ov The nested observable
	 * @return An observable value whose value is the value of <code>ov.get()</code>
	 */
	public static <T> ObservableValue<T> flatten(final Type type, final ObservableValue<? extends ObservableValue<? extends T>> ov) {
		DefaultObservableValue<T> ret = new DefaultObservableValue<T>() {
			@Override
			public Type getType() {
				if(type != null)
					return type;
				else
					return ov.get().getType();
			}

			@Override
			public T get() {
				ObservableValue<? extends T> contained = ov.get();
				return contained == null ? null : contained.get();
			}
		};
		Observer<ObservableValueEvent<T>> controller = ret.control(null);
		ov.act(value -> {
			ObservableValueEvent<T> evt = new ObservableValueEvent<>(ret, value.getOldValue() == null ? null : value.getOldValue().get(),
				value.getValue() == null ? null : value.getValue().get(), value.getCause());
			controller.onNext(evt);
			if(value.getValue() != null)
				value
					.getValue()
					.takeUntil(ov)
					.act(
						value2 -> {
							ObservableValueEvent<T> evt2 = new ObservableValueEvent<>(ret, value2.getOldValue(), value2.getValue(), value2
								.getCause());
							controller.onNext(evt2);
						});
		});
		return ret;
	}

	/**
	 * Assembles an observable value, with changes occuring on the basis of changes to a set of components
	 *
	 * @param type The type of the new value
	 * @param value The function to get the new value on demand
	 * @param components The components whose changes require a new value to be produced
	 * @return The new observable value
	 */
	public static <T> ObservableValue<T> assemble(Type type, java.util.function.Supplier<T> value, ObservableValue<?>... components) {
		Type t = type == null ? ComposedObservableValue.getReturnType(value) : type;
		return new ObservableValue<T>() {
			@Override
			public Type getType() {
				return t;
			}

			@Override
			public T get() {
				return value.get();
			}

			@Override
			public Subscription<ObservableValueEvent<T>> subscribe(Observer<? super ObservableValueEvent<T>> observer) {
				ObservableValue<T> outer = this;
				Subscription<?> [] subSubs = new Subscription[components.length];
				DefaultSubscription<ObservableValueEvent<T>> ret = new DefaultSubscription<ObservableValueEvent<T>>(this) {
					@Override
					public void unsubscribeSelf() {
						for(Subscription<?> sub : subSubs)
							sub.unsubscribe();
					}
				};
				Object [] oldValue = new Object[1];
				for(int i = 0; i < subSubs.length; i++)
					subSubs[i] = components[i].subscribe(new Observer<ObservableValueEvent<?>>() {
						@Override
						public <V extends ObservableValueEvent<?>> void onNext(V value2) {
							T newVal = value.get();
							T oldVal = (T) oldValue[0];
							oldValue[0] = newVal;
							observer.onNext(new ObservableValueEvent<T>(outer, oldVal, newVal, value2.getCause()));
						}

						@Override
						public <V extends ObservableValueEvent<?>> void onCompleted(V value2) {
							ret.unsubscribe();
						}

						@Override
						public void onError(Throwable e) {
							observer.onError(e);
						}
					});
				return ret;
			}
		};
	}

	/**
	 * @param <V> The first argument type
	 * @param <U> The second argument type
	 * @return A binary function that returns its first argument
	 */
	public static <V, U> BiFunction<V, U, V> first() {
		return (V v1, U v2) -> {
			return v1;
		};
	}

	/**
	 * @param <V> The first argument type
	 * @param <U> The second argument type
	 * @return A binary function that returns its second argument
	 */
	public static <V, U> BiFunction<V, U, U> second() {
		return (V v1, U v2) -> {
			return v2;
		};
	}

	/**
	 * An observable value whose value cannot change
	 *
	 * @param <T> The type of this value
	 */
	public static final class ConstantObservableValue<T> implements ObservableValue<T> {
		private final Type theType;
		private final T theValue;

		/**
		 * @param type The type of this observable value
		 * @param value This observable value's value
		 */
		public ConstantObservableValue(Type type, T value) {
			theType = type;
			theValue = value;
		}

		@Override
		public Subscription<ObservableValueEvent<T>> subscribe(Observer<? super ObservableValueEvent<T>> observer) {
			observer.onNext(new ObservableValueEvent<>(this, theValue, theValue, null));
			return new Subscription<ObservableValueEvent<T>>() {
				@Override
				public Subscription<ObservableValueEvent<T>> subscribe(Observer<? super ObservableValueEvent<T>> observer2) {
					return this;
				}

				@Override
				public void unsubscribe() {
				}
			};
		}

		@Override
		public Type getType() {
			return theType;
		}

		@Override
		public T get() {
			return theValue;
		}
	}
}

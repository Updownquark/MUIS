package org.muis.core.rx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A stream of values that can be filtered, mapped, composed, etc. and evaluated on
 *
 * @param <T> The type of values this observable provides
 */
public interface Observable<T> {
	/**
	 * @param observer The observer to listen to this observable
	 * @return A subscription to use to unsubscribe the listener from this observable
	 */
	Subscription<T> subscribe(Observer<? super T> observer);

	/**
	 * @param action The action to perform for each new value
	 * @return The subscription for the action
	 */
	default Subscription<T> act(Action<? super T> action) {
		return subscribe(new Observer<T>() {
			@Override
			public <V extends T> void onNext(V value) {
				action.act(value);
			}
		});
	}

	/** @return An observable for this observable's errors */
	default Observable<Throwable> error() {
		DefaultObservable<Throwable> ret = new DefaultObservable<>();
		Observer<Throwable> controller = ret.control(null);
		subscribe(new Observer<T>() {
			@Override
			public <V extends T> void onNext(V value) {
			}

			@Override
			public void onError(Throwable e) {
				controller.onNext(e);
			}
		});
		return ret;
	}

	/** @return An observable that will fire once when this observable completes (the value will be null) */
	default Observable<T> completed() {
		DefaultObservable<T> ret = new DefaultObservable<>();
		Observer<T> controller = ret.control(null);
		subscribe(new Observer<T>() {
			@Override
			public <V extends T> void onNext(V value) {
			}

			@Override
			public <V extends T> void onCompleted(V value) {
				controller.onNext(value);
			}
		});
		return ret;
	}

	/**
	 * @param func The filter function
	 * @return An observable that provides the same values as this observable minus those that the filter function returns false for
	 */
	default Observable<T> filter(Function<? super T, Boolean> func) {
		DefaultObservable<T> ret=new DefaultObservable<>();
		Observer<T> controller=ret.control(null);
		subscribe(new Observer<T>(){
			@Override
			public <V extends T> void onNext(V value) {
				if(func.apply(value))
					controller.onNext(value);
			}

			@Override
			public <V extends T> void onCompleted(V value) {
				controller.onCompleted(value);
			}

			@Override
			public void onError(Throwable e) {
				controller.onError(e);
			}
		});
		return ret;
	}

	/**
	 * @param <R> The type of the returned observable
	 * @param func The map function
	 * @return An observable that provides the values of this observable, mapped by the given function
	 */
	default <R> Observable<R> map(Function<? super T, R> func) {
		return new ComposedObservable<>(args -> {
			return func.apply((T) args[0]);
		}, this);
	}

	/**
	 * @param <R> The type of the returned observable
	 * @param func The filter map function
	 * @return An observable that provides the values of this observable, mapped by the given function, except where that function returns
	 *         null
	 */
	default <R> Observable<R> filterMap(Function<? super T, R> func) {
		DefaultObservable<R> ret = new DefaultObservable<>();
		Observer<R> controller = ret.control(null);
		subscribe(new Observer<T>() {
			@Override
			public <V extends T> void onNext(V value) {
				R newVal = func.apply(value);
				if(newVal != null)
					controller.onNext(newVal);
			}

			@Override
			public <V extends T> void onCompleted(V value) {
				controller.onCompleted(func.apply(value));
			}

			@Override
			public void onError(Throwable e) {
				controller.onError(e);
			}
		});
		return ret;
	}

	/**
	 * @param <V> The type of the other observable to be combined with this one
	 * @param <R> The type of the returned observable
	 * @param other The other observable to compose
	 * @param func The function to use to combine the observables' values
	 * @return A new observable whose values are the specified combination of this observable and the others'
	 */
	default <V, R> Observable<R> combine(Observable<V> other, BiFunction<? super T, ? super V, R> func) {
		return new ComposedObservable<>(args -> {
			return func.apply((T) args[0], (V) args[1]);
		}, this, other);
	}

	/**
	 * @param until The observable to watch
	 * @return An observable that provides the same values as this observable until the first value is observed from the given observable
	 */
	default Observable<T> takeUntil(Observable<?> until) {
		AtomicBoolean check=new AtomicBoolean(false);
		Observer<T> [] controller=new Observer[1];
		Subscription<?> [] untilSub = new Subscription[1];
		untilSub[0] = until.subscribe(new Observer<Object>() {
			@Override
			public <V> void onNext(V value) {
				if(check.getAndSet(true))
					return;
				if(controller[0]!=null)
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
				if(controller[0]!=null)
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

		DefaultObservable<T> ret = new DefaultObservable<>();
		controller[0] = ret.control(subscriber->{
			if(check.get()) {
				subscriber.onCompleted(null);
				controller[0].onCompleted(null);
				return;
			}
			Subscription<?> [] sub=new Subscription[1];
			sub[0]=subscribe(new Observer<T>(){
				@Override
				public <V extends T> void onNext(V value) {
					controller[0].onNext(value);
				}

				@Override
				public <V extends T> void onCompleted(V value) {
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
	 * @param times The number of values to take from this observable
	 * @return An observable that provides the same values as this observable but completes after the given number of values
	 */
	default Observable<T> take(int times) {
		DefaultObservable<T> ret = new DefaultObservable<>();
		Observer<T> [] controller=new Observer[1];
		controller[0] = ret.control(subscriber->{
			AtomicInteger counter = new AtomicInteger(times);
			Subscription<T> [] sub=new Subscription[1];
			sub[0]=subscribe(new Observer<T>(){
				private boolean isAlive(T value) {
					int count = counter.getAndDecrement();
					if(count == 0) {
						controller[0].onCompleted(value);
					}
					if(count <= 0 && sub[0] != null) {
						sub[0].unsubscribe();
						sub[0] = null;
					}
					return count > 0;
				}

				@Override
				public <V extends T> void onNext(V value) {
					if(isAlive(value))
						controller[0].onNext(value);
				}

				@Override
				public <V extends T> void onCompleted(V value) {
					controller[0].onCompleted(value);
				}

				@Override
				public void onError(Throwable e) {
					if(isAlive(null))
						controller[0].onError(e);
				}
			});
		});
		return ret;
	}

	/**
	 * @param <T> The type of the observable to create
	 * @param value The value for the observable
	 * @return An observable that pushes the given value as soon as it is subscribed to and never completes
	 */
	public static <T> Observable<T> constant(T value) {
		return new Observable<T>() {
			@Override
			public Subscription<T> subscribe(Observer<? super T> observer) {
				observer.onNext(value);
				return nullSubscribe(this);
			}
		};
	}

	/**
	 * @param <T> The type of value the observable returns
	 * @param obs The observer to get compile-time type information for (not used)
	 * @return A subscription that does nothing
	 */
	public static <T> Subscription<T> nullSubscribe(Observable<T> obs) {
		return new Subscription<T>() {
			@Override
			public Subscription<T> subscribe(Observer<? super T> observer) {
				return this;
			}

			@Override
			public void unsubscribe() {
			}
		};
	}
}

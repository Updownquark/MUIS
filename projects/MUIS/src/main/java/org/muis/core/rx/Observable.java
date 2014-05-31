package org.muis.core.rx;

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
			public void onCompleted() {
				controller.onCompleted();
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
			public void onCompleted() {
				controller.onCompleted();
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
		DefaultObservable<T> ret = new DefaultObservable<>();
		Observer<T> controller = ret.control(null);
		Subscription<?> [] mainSub = new Subscription[1];
		Subscription<?> untilSub = until.take(1).act(value -> {
			controller.onCompleted();
			mainSub[0].unsubscribe();
		});
		mainSub[0] = subscribe(new Observer<T>() {
			@Override
			public <V extends T> void onNext(V value) {
				controller.onNext(value);
			}

			@Override
			public void onCompleted() {
				controller.onCompleted();
				untilSub.unsubscribe();
			}

			@Override
			public void onError(Throwable e) {
				controller.onError(e);
			}
		});
		return ret;
	}

	/**
	 * @param times The number of values to take from this observable
	 * @return An observable that provides the same values as this observable but completes after the given number of values
	 */
	default Observable<T> take(int times) {
		AtomicInteger counter = new AtomicInteger(times);
		DefaultObservable<T> ret = new DefaultObservable<>();
		Observer<T> controller = ret.control(null);
		Subscription<T> [] sub = new Subscription[1];
		sub[0] = subscribe(new Observer<T>() {
			@Override
			public <V extends T> void onNext(V value) {
				int count = counter.getAndDecrement();
				if(count >= 0)
					controller.onNext(value);
				if(count == 0) {
					sub[0].unsubscribe();
					controller.onCompleted();
				}
			}

			@Override
			public void onCompleted() {
				controller.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				if(counter.get() > 0)
					controller.onError(e);
			}
		});
		return ret;
	}
}

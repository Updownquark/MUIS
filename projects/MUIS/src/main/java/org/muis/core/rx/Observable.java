package org.muis.core.rx;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface Observable<T> {
	Subscription<T> subscribe(Observer<? super T> observer);

	default Observable<T> act(Action<? super T> action) {
		subscribe(value -> {
			action.act(value);
		});
		return this;
	}

	default Observable<T> filter(Function<T, Boolean> func) {
		DefaultObservable<T> ret=new DefaultObservable<>();
		Observer<T> controller=ret.control(null);
		subscribe(new Observer<T>(){
			@Override
			public void onNext(T value) {
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

	default <R> Observable<R> map(Function<T, R> func) {
		return new ComposedObservable<>(args -> {
			return func.apply((T) args[0]);
		}, this);
	}

	default <R> Observable<R> filterMap(Function<T, R> func) {
		DefaultObservable<R> ret = new DefaultObservable<>();
		Observer<R> controller = ret.control(null);
		subscribe(new Observer<T>() {
			@Override
			public void onNext(T value) {
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

	default <V, R> Observable<R> combine(Observable<V> other, BiFunction<T, V, R> func) {
		return new ComposedObservable<>(args -> {
			return func.apply((T) args[0], (V) args[1]);
		}, this, other);
	}
}

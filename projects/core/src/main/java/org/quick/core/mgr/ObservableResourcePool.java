package org.quick.core.mgr;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.qommons.Transactable;
import org.qommons.Transaction;

import com.google.common.reflect.TypeToken;

public interface ObservableResourcePool extends Transactable {
	<R, S> PooledResourceBuilder<R, S> build(Function<? super R, ? extends S> subscribe, R listener);

	default <T> Observable<T> pool(Observable<T> observable) {
		return new PooledObservable<>(this, observable);
	}

	default <T> ObservableValue<T> poolValue(ObservableValue<T> value) {
		return new PooledValue<>(value, this);
	}

	interface PooledResourceBuilder<R, S> {
		PooledResourceBuilder<R, S> onSubscribe(Consumer<? super R> listener);

		Subscription unsubscribe(BiConsumer<? super R, ? super S> unsubscribe);
	}

	class PooledObservable<T> implements Observable<T> {
		private final ObservableResourcePool thePool;
		private final Observable<T> theObservable;

		public PooledObservable(ObservableResourcePool pool, Observable<T> observable) {
			thePool = pool;
			theObservable = observable;
		}

		@Override
		public Subscription subscribe(Observer<? super T> observer) {
			return thePool.build(theObservable::subscribe, observer).unsubscribe((o, s) -> s.unsubscribe());
		}

		@Override
		public boolean isSafe() {
			return theObservable.isSafe();
		}

		@Override
		public Transaction lock() {
			return theObservable.lock(); // TODO Is there a way to safely avoid locking the observable when the pool is inactive?
		}

		@Override
		public Transaction tryLock() {
			return theObservable.tryLock(); // TODO Is there a way to safely avoid locking the observable when the pool is inactive?
		}

		@Override
		public String toString() {
			return thePool + ": " + theObservable;
		}
	}

	class PooledValue<T> implements ObservableValue<T> {
		private final ObservableValue<T> theWrapped;
		private final ObservableResourcePool thePool;

		public PooledValue(ObservableValue<T> wrapped, ObservableResourcePool pool) {
			theWrapped = wrapped;
			thePool = pool;
		}

		@Override
		public TypeToken<T> getType() {
			return theWrapped.getType();
		}

		@Override
		public T get() {
			return theWrapped.get();
		}

		@Override
		public Observable<ObservableValueEvent<T>> noInitChanges() {
			return new PooledValueChanges();
		}

		private class PooledValueChanges implements Observable<ObservableValueEvent<T>> {
			private final Observable<ObservableValueEvent<T>> thePooledChanges;

			PooledValueChanges() {
				thePooledChanges = thePool.pool(theWrapped.noInitChanges());
			}

			@Override
			public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
				return thePooledChanges.subscribe(new PooledObserver(observer));
			}

			@Override
			public boolean isSafe() {
				return thePooledChanges.isSafe();
			}

			@Override
			public Transaction lock() {
				return thePooledChanges.lock();
			}

			@Override
			public Transaction tryLock() {
				return thePooledChanges.tryLock();
			}
		}

		class PooledObserver implements Observer<ObservableValueEvent<T>> {
			private final Observer<? super ObservableValueEvent<T>> theObserver;
			private T theValue;
			private boolean isInitialized;

			PooledObserver(Observer<? super ObservableValueEvent<T>> observer) {
				theObserver = observer;
			}

			@Override
			public <V extends ObservableValueEvent<T>> void onNext(V value) {
				if (!isInitialized) {
					isInitialized = true;
					theObserver.onNext(value);
				} else if (theValue == value.getOldValue()) {
					if (!value.isInitial())
						theObserver.onNext(value);
				} else
					theObserver.onNext(theWrapped.createChangeEvent(theValue, value.getNewValue(), value));
				theValue = value.getNewValue();
			}

			@Override
			public <V extends ObservableValueEvent<T>> void onCompleted(V value) {
				if (!isInitialized) {
					isInitialized = true;
					theObserver.onCompleted(value);
				} else if (theValue == value.getOldValue())
					theObserver.onCompleted(value);
				else
					theObserver.onCompleted(theWrapped.createChangeEvent(theValue, value.getNewValue(), value));
				theValue = null;
			}
		}
	}
}

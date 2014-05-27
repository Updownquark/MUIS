package org.muis.core.rx;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultObservable<T> implements Observable<T> {
	public static interface OnSubscribe<T> {
		void onsubscribe(Observer<? super T> observer);
	}

	private OnSubscribe<T> theOnSubscribe;
	private AtomicBoolean isAlive = new AtomicBoolean(false);
	private AtomicBoolean hasIssuedController = new AtomicBoolean(false);
	private final CopyOnWriteArrayList<Observer<? super T>> theListeners;

	public DefaultObservable() {
		theListeners = new CopyOnWriteArrayList<>();
	}

	public Observer<T> control(OnSubscribe<T> onSubscribe) throws IllegalStateException {
		if(hasIssuedController.getAndSet(true))
			throw new IllegalStateException("This observable is already controlled");
		theOnSubscribe = onSubscribe;
		return new Observer<T>(){
			@Override
			public void onNext(T value) {
				fireNext(value);
			}

			@Override
			public void onCompleted() {
				fireCompleted();
			}

			@Override
			public void onError(Throwable e) {
				fireError(e);
			}
		};
	}

	@Override
	public Subscription<T> subscribe(Observer<? super T> observer) {
		if(!isAlive.get())
			observer.onCompleted();
		else {
			theListeners.add(observer);
			if(theOnSubscribe != null)
				theOnSubscribe.onsubscribe(observer);
		}
		return new Subscription<T>() {
			@Override
			public Subscription<T> subscribe(Observer<? super T> observer2) {
				return DefaultObservable.this.subscribe(observer2);
			}

			@Override
			public void unsubscribe() {
				theListeners.remove(observer);
			}
		};
	}

	private void fireNext(T value) {
		if(!isAlive.get())
			throw new IllegalStateException("Firing a value on a completed observable");
		for(Observer<? super T> observer : theListeners) {
			try {
				observer.onNext(value);
			} catch(Throwable e) {
				observer.onError(e);
			}
		}
	}

	private void fireCompleted() {
		isAlive.set(false);
		Observer<? super T> [] observers = theListeners.toArray(new Observer[theListeners.size()]);
		theListeners.clear();
		for(Observer<? super T> observer : observers) {
			try {
				observer.onCompleted();
			} catch(Throwable e) {
				observer.onError(e);
			}
		}
	}

	private void fireError(Throwable e) {
		if(!isAlive.get())
			throw new IllegalStateException("Firing a value on a completed observable");
		for(Observer<? super T> observer : theListeners) {
			observer.onError(e);
		}
	}
}

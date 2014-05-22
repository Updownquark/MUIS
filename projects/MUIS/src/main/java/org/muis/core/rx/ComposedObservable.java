package org.muis.core.rx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * An observable that depends on the values of other observables
 *
 * @param <T> The type of the composed observable
 */
public class ComposedObservable<T> implements Observable<T> {
	private static final Object NONE = new Object();

	private volatile boolean isAlive = true;
	private final List<Observable<?>> theComposed;
	private final List<Observer<Object>> theInternalListeners;
	private final List<Object> theInternalValues;
	private final List<Subscription<?>> theInternalSubscriptions;
	private final Function<Object [], T> theFunction;
	private final CopyOnWriteArrayList<Observer<? super T>> theListeners;

	/**
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservable(Function<Object [], T> function, Observable<?>... composed) {
		theFunction = function;
		theComposed = new java.util.ArrayList<>(java.util.Arrays.asList(composed));
		theInternalListeners=new java.util.ArrayList<>(composed.length);
		theInternalValues=new java.util.ArrayList<>(composed.length);
		theInternalSubscriptions=new java.util.ArrayList<>(composed.length);
		theListeners = new CopyOnWriteArrayList<>();
		for(int i=0;i<composed.length;i++){
			final int index=i;
			theInternalValues.add(NONE);
			Observer<Object> listener=new Observer<Object>(){
				@Override
				public void onNext(Object value) {
					theInternalValues.set(index, value);
					fireNext();
				}

				@Override
				public void onCompleted() {
					fireComplete();
				}

				@Override
				public void onError(Throwable e) {
					fireError(e);
				}
			};
			theInternalListeners.add(listener);
			theInternalSubscriptions.add(theComposed.get(index).subscribe(listener));
		}
	}

	@Override
	public Subscription<T> subscribe(final Observer<? super T> listener) {
		theListeners.add(listener);
		return new Subscription<T>() {
			@Override
			public Subscription<T> subscribe(Observer<? super T> observer) {
				return ComposedObservable.this.subscribe(listener);
			}

			@Override
			public void unsubscribe() {
				theListeners.remove(listener);
			}
		};
	}

	private void fireNext() {
		Object [] args = theInternalValues.toArray();
		for(Object value : args)
			if(value == NONE)
				return;
		T next;
		try {
			next = theFunction.apply(args);
		} catch(Throwable e) {
			fireError(e);
			return;
		}
		for(Observer<? super T> listener : theListeners)
			listener.onNext(next);
	}

	private void fireComplete() {
		if(!isAlive)
			return;
		isAlive = false;
		for(Subscription<?> sub : theInternalSubscriptions.toArray(new Subscription[0]))
			sub.unsubscribe();
		theInternalSubscriptions.clear();
	}

	private void fireError(Throwable e) {
		for(Observer<? super T> listener : theListeners)
			listener.onError(e);
	}
}

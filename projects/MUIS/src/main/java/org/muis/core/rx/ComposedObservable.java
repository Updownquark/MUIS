package org.muis.core.rx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * An observable that depends on the values of other observables
 *
 * @param <T> The type of the composed observable
 */
public class ComposedObservable<T> implements Observable<T> {
	private static final Object NONE = new Object();

	private final List<Observable<?>> theComposed;
	private final Function<Object [], T> theFunction;

	/**
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservable(Function<Object [], T> function, Observable<?>... composed) {
		theFunction = function;
		theComposed = new java.util.ArrayList<>(java.util.Arrays.asList(composed));
	}

	@Override
	public Subscription<T> subscribe(Observer<? super T> observer) {
		return new ComposedSubscription(observer);
	}

	/** @return The observables that this observable uses as sources */
	public Observable<?> [] getComposed() {
		return theComposed.toArray(new Observable[theComposed.size()]);
	}

	private class ComposedSubscription extends DefaultSubscription<T> {
		private List<Subscription<?>> theComposedSubs;
		private List<Object> theInternalValues;
		private Observer<T> theObserver;

		ComposedSubscription(Observer<? super T> observer) {
			super(ComposedObservable.this);
			theComposedSubs = new ArrayList<>();
			theInternalValues = new ArrayList<>();
			for(int i = 0; i < theComposed.size(); i++) {
				int index = i;
				theInternalValues.add(NONE);
				theComposedSubs.add(theComposed.get(i).subscribe(new Observer<Object>() {
					@Override
					public <V> void onNext(V value) {
						theInternalValues.set(index, value);
						Object next = getNext();
						if(next != NONE)
							theObserver.onNext((T) value);
					}

					@Override
					public <V> void onCompleted(V value) {
						theInternalValues.set(index, value);
						Object next = getNext();
						if(next != NONE)
							theObserver.onCompleted((T) value);
						unsubscribe();
					}

					@Override
					public void onError(Throwable error) {
						theObserver.onError(error);
					}
				}));
			}
		}

		private Object getNext() {
			Object [] args = theInternalValues.toArray();
			for(Object value : args)
				if(value == NONE)
					return NONE;
			return theFunction.apply(args);
		}

		@Override
		public void unsubscribeSelf() {
			for(Subscription<?> sub : theComposedSubs)
				sub.unsubscribe();
			theComposedSubs.clear();
			theInternalValues.clear();
		}
	}
}

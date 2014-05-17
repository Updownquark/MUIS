package org.muis.core.rx;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * An observable that depends on the values of other observables
 * 
 * @param <T> The type of the composed observable
 */
public class ComposedObservable<T> implements Observable<T> {
	private final List<Observable<?>> theComposed;
	private final Function<Object [], T> theFunction;
	private final CopyOnWriteArrayList<ObservableListener<? super T>> theListeners;

	/**
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservable(Function<Object [], T> function, Observable<?>... composed) {
		theFunction = function;
		theComposed = java.util.Collections.unmodifiableList(java.util.Arrays.asList(composed));
		theListeners = new CopyOnWriteArrayList<>();
		ObservableListener<Object> listener = evt -> {
			Object [] args = new Object[theComposed.size()];
			for(int i = 0; i < args.length; i++) {
				if(theComposed.get(i) == evt.getObservable())
					args[i] = evt.getOldValue();
				else
					args[i] = theComposed.get(i).get();
			}
			T oldValue = theFunction.apply(args);
			for(int i = 0; i < args.length; i++) {
				if(theComposed.get(i) == evt.getObservable())
					args[i] = evt.getNewValue();
			}

			T newValue = theFunction.apply(args);
			ObservableEvent<T> toFire = new ObservableEvent<>(this, oldValue, newValue, evt);
			fire(toFire);
		};
		for(Observable<?> comp : composed)
			comp.addListener(listener);
	}

	@Override
	public Type getType() {
		try {
			return theFunction.getClass().getMethod("apply", Object [].class).getGenericReturnType();
		} catch(NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("No apply method on a function?", e);
		}
	}

	@Override
	public T get() {
		Object [] args = new Object[theComposed.size()];
		for(int i = 0; i < args.length; i++)
			args[i] = theComposed.get(i).get();
		return theFunction.apply(args);
	}

	@Override
	public Observable<T> addListener(ObservableListener<? super T> listener) {
		theListeners.add(listener);
		return this;
	}

	@Override
	public Observable<T> removeListener(ObservableListener<?> listener) {
		theListeners.remove(listener);
		return this;
	}

	private void fire(ObservableEvent<T> event) {
		for(ObservableListener<? super T> listener : theListeners)
			listener.eventOccurred(event);
	}
}

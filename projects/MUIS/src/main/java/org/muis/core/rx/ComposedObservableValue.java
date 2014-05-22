package org.muis.core.rx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * An observable that depends on the values of other observables
 *
 * @param <T> The type of the composed observable
 */
public class ComposedObservableValue<T> implements ObservableValue<T> {
	private final List<ObservableValue<?>> theComposed;
	private final Function<Object [], T> theFunction;
	private final CopyOnWriteArrayList<ObservableValueListener<? super T>> theListeners;

	/**
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservableValue(Function<Object [], T> function, ObservableValue<?>... composed) {
		theFunction = function;
		theComposed = java.util.Collections.unmodifiableList(java.util.Arrays.asList(composed));
		theListeners = new CopyOnWriteArrayList<>();
		ObservableValueListener<Object> listener = evt -> {
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
			ObservableValueEvent<T> toFire = new ObservableValueEvent<>(this, oldValue, newValue, evt);
			fire(toFire);
		};
		for(ObservableValue<?> comp : composed)
			comp.addListener(listener);
	}

	@Override
	public Type getType() {
		try {
			return new Type(theFunction.getClass().getMethod("apply", Object [].class).getGenericReturnType());
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
	public ObservableValue<T> addListener(ObservableValueListener<? super T> listener) {
		theListeners.add(listener);
		return this;
	}

	@Override
	public ObservableValue<T> removeListener(ObservableValueListener<?> listener) {
		theListeners.remove(listener);
		return this;
	}

	private void fire(ObservableValueEvent<T> event) {
		for(ObservableValueListener<? super T> listener : theListeners)
			listener.valueChanged(event);
	}
}

package org.muis.core.rx;

import java.util.List;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * An observable that depends on the values of other observables
 *
 * @param <T> The type of the composed observable
 */
public class ComposedObservableValue<T> extends DefaultObservableValue<T> {
	private final Observer<ObservableValueEvent<T>> theController;
	private final List<ObservableValue<?>> theComposed;
	private final Function<Object [], T> theFunction;
	private final Type theType;

	/**
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservableValue(Function<Object [], T> function, ObservableValue<?>... composed) {
		this(null, function, composed);
	}

	/**
	 * @param type The type for this value
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservableValue(Type type, Function<Object [], T> function, ObservableValue<?>... composed) {
		theFunction = function;
		try {
			theType = type != null ? type : new Type(theFunction.getClass().getMethod("apply", Object.class).getGenericReturnType());
		} catch(NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("No apply method on a function?", e);
		}
		theComposed = java.util.Collections.unmodifiableList(java.util.Arrays.asList(composed));
		theController = control(observer -> {
			fire(observer);
		});
		Observer<ObservableValueEvent<Object>> listener = new Observer<ObservableValueEvent<Object>>() {
			@Override
			public <V extends ObservableValueEvent<Object>> void onNext(V evt) {
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
						args[i] = evt.getValue();
				}

				T newValue = theFunction.apply(args);
				ObservableValueEvent<T> toFire = new ObservableValueEvent<>(ComposedObservableValue.this, oldValue, newValue, evt);
				theController.onNext(toFire);
			}
		};
		for(ObservableValue<?> comp : composed)
			((ObservableValue<Object>) comp).subscribe(listener);
	}

	@Override
	public Type getType() {
		return theType;
	}

	@Override
	public T get() {
		Object [] args = new Object[theComposed.size()];
		for(int i = 0; i < args.length; i++)
			args[i] = theComposed.get(i).get();
		return theFunction.apply(args);
	}

	private void fire(Observer<? super ObservableValueEvent<T>> observer) {
		T value = get();
		ObservableValueEvent<T> event = new ObservableValueEvent<>(this, null, value, null);
		observer.onNext(event);
	}
}

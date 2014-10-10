package org.muis.core.rx;

import java.util.List;
import java.util.function.BiFunction;
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
	private final Type theType;
	private final boolean combineNulls;

	/**
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param combineNull Whether to apply the combination function if the arguments are null. If false and any arguments are null, the
	 *            result will be null.
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservableValue(Function<Object [], T> function, boolean combineNull, ObservableValue<?>... composed) {
		this(null, function, combineNull, composed);
	}

	/**
	 * @param type The type for this value
	 * @param function The function that operates on the argument observables to produce this observable's value
	 * @param combineNull Whether to apply the combination function if the arguments are null. If false and any arguments are null, the
	 *            result will be null.
	 * @param composed The argument observables whose values are passed to the function
	 */
	public ComposedObservableValue(Type type, Function<Object [], T> function, boolean combineNull, ObservableValue<?>... composed) {
		theFunction = function;
		combineNulls = combineNull;
		theType = type != null ? type : getReturnType(function);
		theComposed = java.util.Collections.unmodifiableList(java.util.Arrays.asList(composed));
	}

	@Override
	public Type getType() {
		return theType;
	}

	/** @return The observable values that compose this value */
	public ObservableValue<?> [] getComposed() {
		return theComposed.toArray(new ObservableValue[theComposed.size()]);
	}

	/** @return The function used to map this observable's composed values into its return value */
	public Function<Object [], T> getFunction() {
		return theFunction;
	}

	/**
	 * @return Whether the combination function will be applied if the arguments are null. If false and any arguments are null, the result
	 *         will be null.
	 */
	public boolean isNullCombined() {
		return combineNulls;
	}

	@Override
	public T get() {
		Object [] args = new Object[theComposed.size()];
		for(int i = 0; i < args.length; i++)
			args[i] = theComposed.get(i).get();
		return combine(args);
	}

	private T combine(Object [] args) {
		if(!combineNulls) {
			for(Object arg : args)
				if(arg == null)
					return null;
		}
		return theFunction.apply(args.clone());
	}

	@Override
	public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
		Runnable [] subs = new Runnable[theComposed.size()];
		Object [] args = new Object[theComposed.size()];
		for(int i = 0; i < args.length; i++)
			args[i] = theComposed.get(i).get();
		Object [] oldValue = new Object[] {combine(args)};
		for(int i = 0; i < args.length; i++) {
			int index = i;
			subs[i] = theComposed.get(i).internalSubscribe(new Observer<ObservableValueEvent<?>>() {
				@Override
				public <V extends ObservableValueEvent<?>> void onNext(V event) {
					args[index] = event.getValue();
					T newValue = combine(args);
					ObservableValueEvent<T> toFire = new ObservableValueEvent<>(ComposedObservableValue.this, (T) oldValue[0], newValue,
						event);
					oldValue[0] = newValue;
					observer.onNext(toFire);
				}

				@Override
				public <V extends ObservableValueEvent<?>> void onCompleted(V event) {
					args[index] = event.getValue();
					T newValue = combine(args);
					ObservableValueEvent<T> toFire = new ObservableValueEvent<>(ComposedObservableValue.this, (T) oldValue[0], newValue,
						event);
					oldValue[0] = newValue;
					observer.onCompleted(toFire);
				}

				@Override
				public void onError(Throwable e) {
					observer.onError(e);
				}
			});
		}
		observer.onNext(new ObservableValueEvent<>(this, null, (T) oldValue[0], null));
		return () -> {
			for(Runnable sub : subs)
				sub.run();
		};
	}

	/**
	 * @param function The function
	 * @return The return type of the function
	 */
	public static Type getReturnType(Function<?, ?> function) {
		return getReturnType(function, "apply", Object.class);
	}

	/**
	 * @param function The function
	 * @return The return type of the function
	 */
	public static Type getReturnType(BiFunction<?, ?, ?> function) {
		return getReturnType(function, "apply", Object.class, Object.class);
	}

	/**
	 * @param function The function
	 * @return The return type of the function
	 */
	public static Type getReturnType(java.util.function.Supplier<?> function) {
		return getReturnType(function, "get");
	}

	private static Type getReturnType(Object function, String methodName, Class<?>... types) {
		try {
			return new Type(function.getClass().getMethod(methodName, types).getGenericReturnType());
		} catch(NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("No apply method on a function?", e);
		}
	}
}

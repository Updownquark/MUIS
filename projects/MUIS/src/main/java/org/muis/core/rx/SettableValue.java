package org.muis.core.rx;

import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * An observable value for which a value can be assigned directly
 *
 * @param <T> The type of the value
 */
public interface SettableValue<T> extends ObservableValue<T> {
	/**
	 * @param value The value to assign to this value
	 * @param cause Something that may have caused this change
	 * @return This value, for chaining
	 * @throws IllegalArgumentException If the value is not acceptable or setting it fails
	 */
	<V extends T> SettableValue<T> set(V value, Object cause) throws IllegalArgumentException;

	/**
	 * @param value The value to check
	 * @return null if the value is not known to be unacceptable for this value, or an error text if it is known to be unacceptable. A null
	 *         value returned from this method does not guarantee that a call to {@link #set(Object, Object)} for the same value will not
	 *         throw an IllegalArgumentException
	 */
	<V extends T> String isAcceptable(V value);

	public default <R> SettableValue<R> mapV(Function<? super T, R> function, Function<? super R, ? extends T> reverse) {
		return mapV(null, function, reverse);
	}

	public default <R> SettableValue<R> mapV(Type type, Function<? super T, R> function, Function<? super R, ? extends T> reverse) {
		SettableValue<T> root = this;
		return new ComposedSettableValue<R>(type, args -> {
			return function.apply((T) args[0]);
		}, this) {
			@Override
			public <V extends R> SettableValue<R> set(V value, Object cause) throws IllegalArgumentException {
				root.set(reverse.apply(value), cause);
				return this;
			}

			@Override
			public <V extends R> String isAcceptable(V value) {
				return root.isAcceptable(reverse.apply(value));
			}
		};
	}

	public default <U, R> SettableValue<R> composeV(BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg,
		BiFunction<? super R, ? super U, ? extends T> reverse) {
		return composeV(null, function, arg, reverse);
	}

	public default <U, R> SettableValue<R> composeV(Type type, BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg,
		BiFunction<? super R, ? super U, ? extends T> reverse) {
		SettableValue<T> root = this;
		return new ComposedSettableValue<R>(type, args -> {
			return function.apply((T) args[0], (U) args[1]);
		}, this) {
			@Override
			public <V extends R> SettableValue<R> set(V value, Object cause) throws IllegalArgumentException {
				root.set(reverse.apply(value, arg.get()), cause);
				return this;
			}

			@Override
			public <V extends R> String isAcceptable(V value) {
				return root.isAcceptable(reverse.apply(value, arg.get()));
			}
		};
	}

	public default <U, R> SettableValue<R> composeV(Type type, BiFunction<? super T, ? super U, R> function, ObservableValue<U> arg,
		BiFunction<? super R, ? super U, String> accept, BiFunction<? super R, ? super U, ? extends T> reverse) {
		SettableValue<T> root = this;
		return new ComposedSettableValue<R>(type, args -> {
			return function.apply((T) args[0], (U) args[1]);
		}, this) {
			@Override
			public <V extends R> SettableValue<R> set(V value, Object cause) throws IllegalArgumentException {
				root.set(reverse.apply(value, arg.get()), cause);
				return this;
			}

			@Override
			public <V extends R> String isAcceptable(V value) {
				U argVal = arg.get();
				String ret = accept.apply(value, argVal);
				if(ret == null)
					ret = root.isAcceptable(reverse.apply(value, arg.get()));
				return ret;
			}
		};
	}

	public default <U, V, R> SettableValue<R> composeV(TriFunction<? super T, ? super U, ? super V, R> function, ObservableValue<U> arg2,
		ObservableValue<V> arg3, TriFunction<? super R, ? super U, ? super V, ? extends T> reverse) {
		return composeV(null, function, arg2, arg3, reverse);
	}

	public default <U, V, R> SettableValue<R> composeV(Type type, TriFunction<? super T, ? super U, ? super V, R> function,
		ObservableValue<U> arg2, ObservableValue<V> arg3, TriFunction<? super R, ? super U, ? super V, ? extends T> reverse) {
		SettableValue<T> root = this;
		return new ComposedSettableValue<R>(type, args -> {
			return function.apply((T) args[0], (U) args[1], (V) args[2]);
		}, this) {
			@Override
			public <V extends R> SettableValue<R> set(V value, Object cause) throws IllegalArgumentException {
				root.set(reverse.apply(value, arg2.get(), arg3.get()), cause);
				return this;
			}

			@Override
			public <V extends R> String isAcceptable(V value) {
				return root.isAcceptable(reverse.apply(value, arg2.get(), arg3.get()));
			}
		};
	}
}

abstract class ComposedSettableValue<T> extends ComposedObservableValue<T> implements SettableValue<T> {
	public ComposedSettableValue(Function<Object [], T> function, ObservableValue<?> [] composed) {
		super(function, composed);
	}

	public ComposedSettableValue(Type type, Function<Object [], T> function, ObservableValue<?>... composed) {
		super(type, function, composed);
	}
}

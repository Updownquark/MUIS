package org.quick.core.prop;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.qommons.TriFunction;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

/**
 * Performs some operation on zero or more arguments and potentially produces a value
 *
 * @param <T> The type of value that the function produces
 */
public abstract class ExpressionFunction<T> {
	private final TypeToken<T> theReturnType;
	private final List<TypeToken<?>> theArgumentTypes;
	private final boolean isVarArgs;

	private ExpressionFunction(TypeToken<T> returnType, List<TypeToken<?>> argTypes, boolean varArgs) {
		if (varArgs && argTypes.isEmpty())
			throw new IllegalArgumentException("Cannot have var args with no argument types");
		theReturnType = returnType;
		theArgumentTypes = Collections.unmodifiableList(new ArrayList<>(argTypes));
		isVarArgs = varArgs;
	}

	/** @return The type of value that the function produces */
	public TypeToken<T> getReturnType() {
		return theReturnType;
	}

	/** @return The argument types that this function expects */
	public List<TypeToken<?>> getArgumentTypes() {
		return theArgumentTypes;
	}

	/** @return Whether the last argument may be repeated */
	public boolean isVarArgs() {
		return isVarArgs;
	}

	/**
	 * @param argTypes The argument types
	 * @return Whether the function can apply to an argument list of the given types
	 */
	public boolean applies(List<TypeToken<?>> argTypes) {
		if (isVarArgs) {
			if (argTypes.size() < theArgumentTypes.size() - 1)
				return false;
		} else {
			if (argTypes.size() != theArgumentTypes.size())
				return false;
		}
		int i;
		for (i = 0; i < theArgumentTypes.size(); i++) {
			if (isVarArgs && i == theArgumentTypes.size() - 1)
				break;
			if (!QuickUtils.isAssignableFrom(theArgumentTypes.get(i), argTypes.get(i)))
				return false;
		}
		if (isVarArgs) {
			TypeToken<?> varArgType = theArgumentTypes.get(theArgumentTypes.size() - 1);
			for (; i < argTypes.size(); i++) {
				if (!QuickUtils.isAssignableFrom(varArgType, argTypes.get(i)))
					return false;
			}
		}
		return true;
	}

	/**
	 * Applies the function
	 *
	 * @param values The parameters to apply the function to
	 * @return The return value
	 */
	public abstract T apply(List<?> values);

	/**
	 * @param returnType The return type for the function
	 * @return A builder for a function
	 */
	public static <T> Builder<T> build(TypeToken<T> returnType) {
		return new Builder<>(returnType);
	}

	/**
	 * @param fn The supplier to wrap as a function
	 * @return A function that takes no arguments and uses the given supplier to produce a value
	 */
	public static <T> ExpressionFunction<T> build(Supplier<T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {}, Collections.emptyList(), args -> fn.get());
	}

	/**
	 * @param fn The function to wrap as an {@link ExpressionFunction}
	 * @return An {@link ExpressionFunction} that takes an argument and returns the value produced by the given function
	 */
	public static <A, T> ExpressionFunction<T> build(Function<A, T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {}, Arrays.asList(//
			new TypeToken<A>(fn.getClass()) {}//
		), args -> fn.apply((A) args.get(0)));
	}

	/**
	 * @param fn The function to wrap as an {@link ExpressionFunction}
	 * @return An {@link ExpressionFunction} that takes 2 arguments and returns the value produced by the given function
	 */
	public static <A1, A2, T> ExpressionFunction<T> build(BiFunction<A1, A2, T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {},
			Arrays.asList(//
				new TypeToken<A1>(fn.getClass()) {}, //
				new TypeToken<A2>(fn.getClass()) {}//
			), args -> fn.apply((A1) args.get(0), (A2) args.get(1)));
	}

	/**
	 * @param fn The function to wrap as an {@link ExpressionFunction}
	 * @return An {@link ExpressionFunction} that takes 3 arguments and returns the value produced by the given function
	 */
	public static <A1, A2, A3, T> ExpressionFunction<T> build(TriFunction<A1, A2, A3, T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {},
			Arrays.asList(//
				new TypeToken<A1>(fn.getClass()) {}, //
				new TypeToken<A2>(fn.getClass()) {}, //
				new TypeToken<A3>(fn.getClass()) {}//
			), args -> fn.apply((A1) args.get(0), (A2) args.get(1), (A3) args.get(2)));
	}

	private static <T> ExpressionFunction<T> build(TypeToken<T> returnType, List<TypeToken<?>> argTypes, Function<List<?>, T> apply) {
		return build(returnType).withArgs(argTypes).withApply(apply).build();
	}

	/**
	 * Builds an {@link ExpressionFunction}
	 *
	 * @param <T> The return type of the function
	 */
	public static class Builder<T> {
		private final TypeToken<T> theReturnType;
		private final List<TypeToken<?>> theArgumentTypes;
		private boolean isVarArgs;
		private Function<List<?>, T> theApply;

		private Builder(TypeToken<T> returnType) {
			theReturnType = returnType;
			theArgumentTypes = new ArrayList<>();
		}

		/**
		 * @param argTypes The argument types for the function
		 * @return This builder
		 */
		public Builder<T> withArgs(TypeToken<?>... argTypes) {
			return withArgs(Arrays.asList(argTypes));
		}

		/**
		 * @param argTypes The argument types for the function
		 * @return This builder
		 */
		public Builder<T> withArgs(Collection<TypeToken<?>> argTypes) {
			theArgumentTypes.addAll(argTypes);
			return this;
		}

		/**
		 * Marks the function as var-args
		 *
		 * @return This builder
		 */
		public Builder<T> withVarArgs() {
			isVarArgs = true;
			return this;
		}

		/**
		 * @param apply The logic for the function
		 * @return This builder
		 */
		public Builder<T> withApply(Function<List<?>, T> apply) {
			theApply = apply;
			return this;
		}

		/**
		 * Builds the function
		 *
		 * @return The new function
		 */
		public ExpressionFunction<T> build() {
			final Function<List<?>, T> apply = theApply;
			if (apply == null)
				throw new IllegalStateException("No apply set");
			return new ExpressionFunction<T>(theReturnType, theArgumentTypes, isVarArgs) {
				@Override
				public T apply(List<?> values) {
					return apply.apply(values);
				}
			};
		}
	}
}

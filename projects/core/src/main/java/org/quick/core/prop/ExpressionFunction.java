package org.quick.core.prop;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.qommons.TriFunction;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

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

	public TypeToken<T> getReturnType() {
		return theReturnType;
	}

	public List<TypeToken<?>> getArgumentTypes() {
		return theArgumentTypes;
	}

	public boolean isVarArgs() {
		return isVarArgs;
	}

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

	public abstract T apply(List<?> values);

	public static <T> Builder<T> build(TypeToken<T> returnType) {
		return new Builder<>(returnType);
	}

	public static <T> ExpressionFunction<T> build(Supplier<T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {}, Collections.emptyList(), args -> fn.get());
	}

	public static <A, T> ExpressionFunction<T> build(Function<A, T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {}, Arrays.asList(//
			new TypeToken<A>(fn.getClass()) {}//
		), args -> fn.apply((A) args.get(0)));
	}

	public static <A1, A2, T> ExpressionFunction<T> build(BiFunction<A1, A2, T> fn) {
		return build(new TypeToken<T>(fn.getClass()) {},
			Arrays.asList(//
				new TypeToken<A1>(fn.getClass()) {}, //
				new TypeToken<A2>(fn.getClass()) {}//
			), args -> fn.apply((A1) args.get(0), (A2) args.get(1)));
	}

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

	public static class Builder<T> {
		private final TypeToken<T> theReturnType;
		private final List<TypeToken<?>> theArgumentTypes;
		private boolean isVarArgs;
		private Function<List<?>, T> theApply;

		private Builder(TypeToken<T> returnType) {
			theReturnType = returnType;
			theArgumentTypes = new ArrayList<>();
		}

		public Builder<T> withArgs(TypeToken<?>... argTypes) {
			return withArgs(Arrays.asList(argTypes));
		}

		public Builder<T> withArgs(Collection<TypeToken<?>> argTypes) {
			theArgumentTypes.addAll(argTypes);
			return this;
		}

		public Builder<T> withVarArgs() {
			isVarArgs = true;
			return this;
		}

		public Builder<T> withApply(Function<List<?>, T> apply) {
			theApply = apply;
			return this;
		}

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

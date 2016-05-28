package org.quick.core.prop;

import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.qommons.TriFunction;

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

	public boolean applies(List<ExpressionResult<?>> argTypes) {
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
			if (!isAssignableFrom(theArgumentTypes.get(i), argTypes.get(i)))
				return false;
		}
		if (isVarArgs) {
			TypeToken<?> varArgType = theArgumentTypes.get(theArgumentTypes.size() - 1);
			for (; i < argTypes.size(); i++) {
				if (!isAssignableFrom(varArgType, argTypes.get(i)))
					return false;
			}
		}
		return true;
	}

	private static boolean isAssignableFrom(TypeToken<?> argType, ExpressionResult<?> arg) {
		if (arg.type.isNull) {
			return !argType.isPrimitive();
		}
		return QuickPropertyType.isAssignableFrom(argType, arg.type.type);
	}

	public abstract T apply(List<?> values);

	public static <T> Builder<T> build(TypeToken<T> returnType) {
		return new Builder<>(returnType);
	}

	public static <T> ExpressionFunction<T> build(Supplier<T> fn) {
		return build(fn, Supplier.class, args -> fn.get());
	}

	public static <T> ExpressionFunction<T> build(Function<?, T> fn) {
		return build(fn, Function.class, args -> ((Function<Object, T>) fn).apply(args.get(0)));
	}

	public static <T> ExpressionFunction<T> build(BiFunction<?, ?, T> fn) {
		return build(fn, BiFunction.class, args -> ((BiFunction<Object, Object, T>) fn).apply(args.get(0), args.get(1)));
	}

	public static <T> ExpressionFunction<T> build(TriFunction<?, ?, ?, T> fn) {
		return build(fn, BiFunction.class,
			args -> ((TriFunction<Object, Object, Object, T>) fn).apply(args.get(0), args.get(1), args.get(2)));
	}

	private static <T> ExpressionFunction<T> build(Object fn, Class<?> fnClass, Function<List<?>, T> apply) {
		TypeToken<?> fnType = TypeToken.of(fn.getClass());
		TypeVariable<?>[] tps = fnClass.getTypeParameters();
		Builder<T> builder = build((TypeToken<T>) fnType.resolveType(tps[tps.length - 1]));
		ArrayList<TypeToken<?>> argType = new ArrayList<>(1);
		argType.add(null);
		for (int i = 0; i < tps.length - 1; i++) {
			argType.set(0, fnType.resolveType(tps[i]));
			builder.withArgs(argType);
		}
		builder.withApply(apply);
		return builder.build();
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

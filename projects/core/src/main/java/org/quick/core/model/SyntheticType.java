package org.quick.core.model;

import java.util.*;

import org.observe.ObservableValue;
import org.quick.core.prop.ExpressionFunction;

import com.google.common.reflect.TypeToken;

public class SyntheticType<T> extends TypeToken<T> {
	private final Map<String, ObservableValue<?>> theFields;
	private final Map<String, List<ExpressionFunction<?>>> theMethods;

	private SyntheticType(Class<T> type, Map<String, ObservableValue<?>> fields, Map<String, List<ExpressionFunction<?>>> methods) {
		super(type);
		theFields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
		Map<String, List<ExpressionFunction<?>>> fns = new LinkedHashMap<>();
		for (Map.Entry<String, List<ExpressionFunction<?>>> fn : methods.entrySet()) {
			fns.put(fn.getKey(), Collections.unmodifiableList(new ArrayList<>(fn.getValue())));
		}
		theMethods = Collections.unmodifiableMap(fns);
	}

	// TODO methods to use fields and methods

	public static <T> Builder<T> build(Class<T> type) {
		return new Builder<>(type);
	}

	public static class Builder<T> {
		private final Class<T> theType;
		private final Map<String, ObservableValue<?>> theFields;
		private final Map<String, List<ExpressionFunction<?>>> theMethods;

		private Builder(Class<T> type) {
			theType = type;
			theFields = new LinkedHashMap<>();
			theMethods = new LinkedHashMap<>();
		}

		public Builder<T> withField(String name, ObservableValue<?> value) {
			theFields.put(name, value);
			return this;
		}

		public Builder<T> withFunction(String name, ExpressionFunction<?> method) {
			if (method.getArgumentTypes().isEmpty() || !method.getArgumentTypes().get(0).isAssignableFrom(theType))
				throw new IllegalArgumentException("Methods in a synthetic type must take a value of the type as the first argument");
			List<ExpressionFunction<?>> fns = theMethods.get(name);
			if (fns == null) {
				fns = new ArrayList<>(1);
				theMethods.put(name, fns);
			}
			fns.add(method);
			return this;
		}

		public SyntheticType<T> build() {
			return new SyntheticType<>(theType, theFields, theMethods);
		}
	}
}

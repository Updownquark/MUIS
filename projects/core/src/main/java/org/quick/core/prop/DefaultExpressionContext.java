package org.quick.core.prop;

import java.util.*;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.quick.core.model.SyntheticType;

public class DefaultExpressionContext implements ExpressionContext {
	private final List<ExpressionContext> theParents;
	private final Map<String, ObservableValue<?>> theValues;
	private final List<Function<String, ObservableValue<?>>> theValueGetters;
	private final Map<String, ExpressionType<?>> theTypes;
	private final Map<String, List<ExpressionFunction<?>>> theFunctions;
	private final List<SyntheticType<?>> theSyntheticTypes;

	private DefaultExpressionContext(List<ExpressionContext> parents, Map<String, ObservableValue<?>> values,
		List<Function<String, ObservableValue<?>>> valueGetters, Map<String, ExpressionType<?>> types,
		Map<String, List<ExpressionFunction<?>>> functions, List<SyntheticType<?>> syntheticTypes) {
		theParents = Collections.unmodifiableList(new ArrayList<>(parents));
		theValues = Collections.unmodifiableMap(new LinkedHashMap<>(values));
		theValueGetters = Collections.unmodifiableList(valueGetters);
		theTypes = Collections.unmodifiableMap(new LinkedHashMap<>(types));
		Map<String, List<ExpressionFunction<?>>> fns = new LinkedHashMap<>();
		for (Map.Entry<String, List<ExpressionFunction<?>>> fn : functions.entrySet()) {
			fns.put(fn.getKey(), Collections.unmodifiableList(new ArrayList<>(fn.getValue())));
		}
		theFunctions = Collections.unmodifiableMap(fns);
		theSyntheticTypes = Collections.unmodifiableList(new ArrayList<>(syntheticTypes));
	}

	@Override
	public ExpressionResult<?> getVariable(String name) {
		ObservableValue<?> value = theValues.get(name);
		if (value != null)
			return ExpressionResult.of(value);
		ExpressionType<?> type = theTypes.get(name);
		if (type != null)
			return ExpressionResult.ofType(type);
		for (Function<String, ObservableValue<?>> getter : theValueGetters) {
			value = getter.apply(name);
			if (value != null)
				return ExpressionResult.of(value);
		}
		for (ExpressionContext parent : theParents) {
			ExpressionResult<?> res = parent.getVariable(name);
			if (res != null)
				return res;
		}
		return null;
	}

	@Override
	public void getFunctions(String name, List<ExpressionResult<?>> args, List<ExpressionFunction<?>> functions) {
		List<ExpressionFunction<?>> fns = theFunctions.get(name);
		if (fns != null) {
			for (ExpressionFunction<?> fn : fns)
				if (fn.applies(args))
					functions.add(fn);
		}
		for (ExpressionContext ctx : theParents)
			ctx.getFunctions(name, args, functions);
	}

	public static Builder build() {
		return new Builder();
	}

	public static class Builder {
		private final List<ExpressionContext> theParents;
		private final Map<String, ObservableValue<?>> theValues;
		private final List<Function<String, ObservableValue<?>>> theValueGetters;
		private final Map<String, ExpressionType<?>> theTypes;
		private final Map<String, List<ExpressionFunction<?>>> theFunctions;
		private final List<SyntheticType<?>> theSyntheticTypes;

		private Builder() {
			theParents = new ArrayList<>();
			theValues = new LinkedHashMap<>();
			theValueGetters = new ArrayList<>();
			theTypes = new LinkedHashMap<>();
			theFunctions = new LinkedHashMap<>();
			theSyntheticTypes = new ArrayList<>();
		}

		public Builder withParent(ExpressionContext ctx) {
			theParents.add(ctx);
			return this;
		}

		public Builder withValue(String name, ObservableValue<?> value) {
			theValues.put(name, value);
			return this;
		}

		public Builder withValueGetter(Function<String, ObservableValue<?>> getter) {
			theValueGetters.add(getter);
			return this;
		}

		public Builder withType(String name, ExpressionType<?> type) {
			theTypes.put(name, type);
			return this;
		}

		public Builder withFunction(String name, ExpressionFunction<?> fn) {
			List<ExpressionFunction<?>> fns = theFunctions.get(name);
			if (fns == null) {
				fns = new ArrayList<>(1);
				theFunctions.put(name, fns);
			}
			fns.add(fn);
			return this;
		}

		public Builder withType(SyntheticType<?> type) {
			theSyntheticTypes.add(type);
			return this;
		}

		public Builder copy(){
			Builder newBuilder=new Builder();
			newBuilder.theParents.addAll(theParents);
			newBuilder.theValues.putAll(theValues);
			newBuilder.theValueGetters.addAll(theValueGetters);
			newBuilder.theTypes.putAll(theTypes);
			newBuilder.theFunctions.putAll(theFunctions);
			newBuilder.theSyntheticTypes.addAll(theSyntheticTypes);
			return newBuilder;
		}

		public DefaultExpressionContext build() {
			return new DefaultExpressionContext(theParents, theValues, theValueGetters, theTypes, theFunctions, theSyntheticTypes);
		}
	}
}

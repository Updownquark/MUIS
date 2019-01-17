package org.quick.core.style;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.util.TypeTokens;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

/** A StyleSheet that cannot be modified */
public class ImmutableStyleSheet implements StyleSheet {
	private final Map<StyleAttribute<?>, ObservableSortedSet<? extends StyleConditionValue<?>>> theValues;

	private ImmutableStyleSheet(Map<StyleAttribute<?>, SortedSet<? extends StyleConditionValue<?>>> values) {
		Map<StyleAttribute<?>, ObservableSortedSet<? extends StyleConditionValue<?>>> vCopy = new LinkedHashMap<>(values.size());
		for (Map.Entry<StyleAttribute<?>, SortedSet<? extends StyleConditionValue<?>>> entry : values.entrySet())
			vCopy.put(entry.getKey(), makeSCVSet(entry.getKey(), entry.getValue()));
		theValues = Collections.unmodifiableMap(vCopy);
	}

	private <T> ObservableSortedSet<StyleConditionValue<T>> makeSCVSet(StyleAttribute<T> key,
		SortedSet<? extends StyleConditionValue<?>> values) {
		return ObservableSortedSet.<StyleConditionValue<T>> of(
			TypeTokens.get().keyFor(StyleConditionValue.class).getCompoundType(key.getType().getType()), //
			StyleConditionValue::compareTo, (SortedSet<StyleConditionValue<T>>) values);
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return ObservableSet.of(new TypeToken<StyleAttribute<?>>() {}, theValues.keySet());
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		ObservableSortedSet<? extends StyleConditionValue<?>> conditions = theValues.get(attr);
		return conditions != null ? (ObservableSortedSet<StyleConditionValue<T>>) conditions : ObservableSortedSet.of(
			TypeTokens.get().keyFor(StyleConditionValue.class).getCompoundType(attr.getType().getType()), StyleConditionValue::compareTo);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append('{');
		boolean first = true;
		for (StyleAttribute<?> att : attributes()) {
			if (!first)
				str.append(", ");
			first = false;
			str.append(att).append('=').append(getStyleExpressions(att));
		}
		str.append('}');
		return str.toString();
	}

	/**
	 * @param msg The message center to log invalid attribute values
	 * @return A builder to build an {@link ImmutableStyleSheet}
	 */
	public static Builder build(QuickMessageCenter msg) {
		return new Builder(msg);
	}

	/** Builds {@link ImmutableStyleSheet}s */
	public static class Builder implements ConditionalStyleSetter {
		private final QuickMessageCenter theMessageCenter;
		private final Map<StyleAttribute<?>, SortedSet<? extends StyleConditionValue<?>>> theValues;

		private Builder(QuickMessageCenter msg) {
			theMessageCenter = msg;
			theValues = new LinkedHashMap<>();
		}

		@Override
		public <T> Builder setConstant(StyleAttribute<T> attr, T value) {
			return (Builder) ConditionalStyleSetter.super.setConstant(attr, value);
		}

		@Override
		public <T> Builder set(StyleAttribute<T> attr, ObservableValue<? extends T> value) {
			return (Builder) ConditionalStyleSetter.super.set(attr, value);
		}

		@Override
		public <T> Builder set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value) {
			if (!QuickUtils.isAssignableFrom(attr.getType().getType(), value.getType()))
				throw new IllegalArgumentException("Incompatible types: " + attr.getType().getType() + " and " + value.getType());
			((SortedSet<StyleConditionValue<?>>) theValues.computeIfAbsent(attr, a -> new TreeSet<>()))
				.add(new StyleConditionValueImpl<>(attr, condition, value, theMessageCenter));
			return this;
		}

		/** @return The new {@link ImmutableStyleSheet} */
		public ImmutableStyleSheet build() {
			return new ImmutableStyleSheet(theValues);
		}
	}
}

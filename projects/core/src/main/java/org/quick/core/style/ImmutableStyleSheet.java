package org.quick.core.style;

import java.util.*;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeParameter;
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
		TypeToken<StyleConditionValue<T>> type = new TypeToken<StyleConditionValue<T>>() {}.where(new TypeParameter<T>() {},
			key.getType().getType());
		return ObservableSortedSet.<StyleConditionValue<T>> constant(type, (SortedSet<StyleConditionValue<T>>) values,
			(o1, o2) -> o1.compareTo(o2));
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return ObservableSet.constant(new TypeToken<StyleAttribute<?>>() {}, theValues.keySet());
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		ObservableSortedSet<? extends StyleConditionValue<?>> conditions = theValues.get(attr);
		return (ObservableSortedSet<StyleConditionValue<T>>) (conditions != null ? conditions
			: ObservableSortedSet.empty(new TypeToken<StyleConditionValue<?>>() {}));
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
		public <T> Builder set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value) {
			if (!attr.getType().getType().isAssignableFrom(value.getType()))
				throw new IllegalArgumentException("Incompatible types: " + attr.getType().getType() + " and " + value.getType());
			((SortedSet<StyleConditionValue<?>>) theValues.computeIfAbsent(attr, a -> new TreeSet<>()))
				.add(new StyleConditionValue<>(attr, condition, value, theMessageCenter));
			return this;
		}

		/** @return The new {@link ImmutableStyleSheet} */
		public ImmutableStyleSheet build() {
			return new ImmutableStyleSheet(theValues);
		}
	}
}

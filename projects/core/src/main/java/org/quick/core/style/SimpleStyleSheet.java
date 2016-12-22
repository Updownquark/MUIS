package org.quick.core.style;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.ObservableValue;
import org.observe.assoc.ObservableMultiMap.ObservableMultiEntry;
import org.observe.assoc.impl.ObservableMultiMapImpl;
import org.observe.collect.CollectionSession;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.collect.impl.ObservableHashSet;
import org.qommons.Transactable;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/** A simple, mutable stylesheet implementation */
public class SimpleStyleSheet implements MutableStyleSheet {
	private final ObservableMultiMapImpl<StyleAttribute<?>, StyleConditionValue<?>> theValues;
	private QuickMessageCenter theMsg;

	/** @param msg The message center to use to report style value errors */
	public SimpleStyleSheet(QuickMessageCenter msg) {
		theValues = new ObservableMultiMapImpl<>(//
			new TypeToken<StyleAttribute<?>>() {}, //
			new TypeToken<StyleConditionValue<?>>() {}, //
			new TypeToken<ObservableMultiMapImpl.SortedSetMultiEntry<StyleAttribute<?>, StyleConditionValue<?>>>() {}, //
			ObservableHashSet.creator(), //
			(key, keyType, valueType, lock, session, controller) -> createEntry((StyleAttribute<Object>) key, keyType, valueType, lock,
				session, controller));
		theMsg = msg;
	}

	private static <T> ObservableMultiEntry<StyleAttribute<?>, StyleConditionValue<?>> createEntry(StyleAttribute<T> key,
		TypeToken<StyleAttribute<?>> keyType, TypeToken<StyleConditionValue<?>> valueType, ReentrantReadWriteLock lock,
		ObservableValue<CollectionSession> session, Transactable controller) {
		TypeToken<StyleConditionValue<T>> attValueType = new TypeToken<StyleConditionValue<T>>() {}.where(new TypeParameter<T>() {},
			key.getType().getType());
		return (ObservableMultiEntry<StyleAttribute<?>, StyleConditionValue<?>>) (ObservableMultiEntry<?, ?>) new ObservableMultiMapImpl.SortedSetMultiEntry<>(
			key, attValueType, lock, session, controller, StyleConditionValue::compareTo);
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theValues.keySet();
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		return ((ObservableSortedSet<StyleConditionValue<?>>) theValues.get(attr)).mapEquivalent(
			new TypeToken<StyleConditionValue<T>>() {}.where(new TypeParameter<T>() {}, attr.getType().getType()),
			v -> (StyleConditionValue<T>) v, v -> v);
	}

	@Override
	public <T> SimpleStyleSheet set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value) {
		theValues.add(attr, new StyleConditionValue<>(attr, condition, value, theMsg));
		return this;
	}

	@Override
	public SimpleStyleSheet clear(StyleAttribute<?> attr, StyleCondition condition) {
		theValues.remove(attr, new StyleConditionValue<>(attr, condition, null, theMsg));
		return this;
	}
}

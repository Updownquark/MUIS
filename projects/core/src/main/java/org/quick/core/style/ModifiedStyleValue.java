package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.assoc.ObservableMap;
import org.observe.collect.ObservableOrderedCollection;
import org.observe.collect.impl.CachingLinkedList;
import org.qommons.ListenerSet;

import com.google.common.reflect.TypeToken;

public class ModifiedStyleValue<T> implements ObservableValue<T> {
	private final ObservableOrderedCollection<ModifierHolder<T>> theModifiers;
	private final ObservableValue<T> theValue;
	private final ObservableMap<StyleModifierProperty<?>, Object> theProperties;
	private final ListenerSet<Observer<? super ObservableValueEvent<T>>> theListeners;

	public ModifiedStyleValue(ObservableValue<T> value, ObservableMap<StyleModifierProperty<?>, Object> properties) {
		theValue = value;
		theProperties = properties;
		theListeners = new ListenerSet<>();
		theListeners.setUsedListener(used -> {
			if (used) {
			} else {
			}
			// TODO
		});
		theListeners.setOnSubscribe(observer -> Observer.onNextAndFinish(observer, createInitialEvent(get(), null)));
		ObservableOrderedCollection<ModifierHolder<T>> uncachedModifiers = ((ObservableOrderedCollection<StyleModifierProperty<?>>) properties
			.keySet()).;
		theModifiers = new CachingLinkedList<>(uncachedModifiers);
	}

	@Override
	public TypeToken<T> getType() {
		return theValue.getType();
	}

	@Override
	public boolean isSafe() {
		return theValue.isSafe();
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
		theListeners.add(observer);
		return () -> theListeners.remove(observer);
	}

	private static class ModifierHolder<T> {
		private final StyleModifier theModifier;
		private final String theName;
		private final ObservableMap<StyleModifierProperty<?>, Object> theProperties;
		private T theModifiedValue;

		ModifierHolder(StyleModifier modifier, String name, ObservableMap<StyleModifierProperty<?>, Object> properties) {
			theModifier = modifier;
			theName = name;
			theProperties = properties;
		}
	}
}

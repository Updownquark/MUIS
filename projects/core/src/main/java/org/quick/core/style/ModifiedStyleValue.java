package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.assoc.ObservableMap;
import org.observe.collect.ObservableOrderedCollection;
import org.observe.collect.ObservableSortedSet;
import org.observe.collect.impl.CachingLinkedList;
import org.qommons.BiTuple;
import org.qommons.ListenerSet;
import org.quick.core.prop.QuickProperty;

import com.google.common.reflect.TypeToken;

public class ModifiedStyleValue<T> implements ObservableValue<T> {
	private final QuickProperty<? super T> theProperty;
	private final ObservableSortedSet<ModifierInstance<T>> theModifiers;
	private final ObservableValue<T> theValue;
	private final ObservableMap<AppliedStyleModifierProperty<?>, Object> theProperties;
	private final ListenerSet<Observer<? super ObservableValueEvent<T>>> theListeners;

	public ModifiedStyleValue(QuickProperty<? super T> property, ObservableValue<T> value, ObservableMap<AppliedStyleModifierProperty<?>, Object> properties) {
		theProperty=property;
		theValue = value;
		theProperties = properties;
		theListeners = new ListenerSet<>();
		theListeners.setOnSubscribe(observer -> Observer.onNextAndFinish(observer, createInitialEvent(get(), null)));

		ObservableOrderedCollection<BiTuple<Class<? extends StyleModifier>, String>> modifierInstances;
		modifierInstances = ((ObservableOrderedCollection<AppliedStyleModifierProperty<?>>) properties.keySet())
			.map(p -> new BiTuple<>(p.getProperty().getModifierType(), p.getInstanceName()));
		theListeners.setUsedListener(used -> {
			if (used) {
			} else {
			}
			// TODO
		});

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

	public static class ModifierInstance<T> implements Comparable<ModifierInstance<?>> {
		private final QuickProperty<? super T> theProperty;
		private final StyleModifier theModifier;
		private final String theName;
		private final ObservableMap<AppliedStyleModifierProperty<?>, Object> theProperties;
		private T theModifiedValue;

		ModifierInstance(QuickProperty<? super T> property, StyleModifier modifier, String name,
			ObservableMap<AppliedStyleModifierProperty<?>, Object> properties) {
			theProperty = property;
			theModifier = modifier;
			theName = name;
			theProperties = properties;
		}

		@Override
		public int compareTo(ModifierInstance<?> o) {

		}
	}

	public static <T> ObservableValue<T> modify(QuickProperty<? super T> property, ObservableValue<T> value,
		ObservableMap<AppliedStyleModifierProperty<?>, Object> properties) {}
}

/*
priority for modifier properties is on modifier type priority, condition order

Convert the style sheet's multi-map of modifier properties to style condition values to a map of property to value
	Use the highest-priority value
Convert the property/value map to an ordered set of style modifiers with their properties:
	Group unique by distinct property type and name (modifier instance)
	Filter out present=false
	Instantiate the modifier instance
	Give it the observable map of properties, filtered by its type and name
	Re-order by type priority, preserving style condition order
	Cache the values to avoid re-instantiating the modifiers each time and to allow preservation of state for each modifier for the property

Style attributes and style modifier properties are evaluated using a flatten of a conditional.
	If there are no modifiers, then the straight value is used.  Otherwise a new ModifiedStyleValue is created (and cached).
 */

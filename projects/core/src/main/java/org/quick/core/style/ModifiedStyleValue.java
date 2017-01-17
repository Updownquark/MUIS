package org.quick.core.style;

import java.util.concurrent.ConcurrentHashMap;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.assoc.ObservableMultiMap;

import com.google.common.reflect.TypeToken;

public class ModifiedStyleValue<T> implements ObservableValue<T> {
	private final ConcurrentHashMap<Class<? extends StyleModifier>, ModifierHolder> theModifiers;
	private final ObservableValue<T> theValue;

	public ModifiedStyleValue(ObservableValue<T> value, ObservableMultiMap<StyleModifierProperty<?>, StyleModifierProperty<?>> properties) {
		theModifiers = new ConcurrentHashMap<>();
		theValue = value;
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSafe() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TypeToken<T> getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T get() {
		// TODO Auto-generated method stub
		return null;
	}

	private class ModifierHolder {}
}

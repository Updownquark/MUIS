package org.muis.core.model;

import java.util.List;

import org.muis.core.MuisElement;
import org.muis.core.rx.ObservableValueListener;

import prisms.lang.Type;

/**
 * The default implementation of MuisModelValue
 *
 * @param <T> The (compile-time) type of the value
 */
public class DefaultMuisModelValue<T> implements MuisModelValue<T>, WidgetRegister {
	private final Type theType;

	private T theValue;

	private List<ObservableValueListener<? super T>> theListeners;

	private List<MuisElement> theRegisteredElements;

	/** @param type The (run-time) type of the value */
	public DefaultMuisModelValue(Class<T> type) {
		this(new Type(type));
	}

	/** @param type The (run-time) type of the value */
	public DefaultMuisModelValue(Type type) {
		theType = type;
		theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		theRegisteredElements = new java.util.concurrent.CopyOnWriteArrayList<>();
	}

	@Override
	public Type getType() {
		return theType;
	}

	@Override
	public T get() {
		return theValue;
	}

	@Override
	public void set(T value, org.muis.core.event.UserEvent userEvent) {
		T oldValue = theValue;
		theValue = value;
		MuisModelValueEvent<T> event = new MuisModelValueEvent<>(this, userEvent, oldValue, value);
		for(ObservableValueListener<? super T> listener : theListeners)
			listener.valueChanged(event);
	}

	@Override
	public DefaultMuisModelValue<T> addListener(ObservableValueListener<? super T> listener) {
		if(listener != null)
			theListeners.add(listener);
		return this;
	}

	@Override
	public DefaultMuisModelValue<T> removeListener(ObservableValueListener<?> listener) {
		theListeners.remove(listener);
		return this;
	}

	@Override
	public WidgetRegistration register(final MuisElement widget) {
		if(widget == null)
			return null;
		theRegisteredElements.add(widget);
		return () -> {
			theRegisteredElements.remove(widget);
		};
	}

	@Override
	public List<MuisElement> registered() {
		return java.util.Collections.unmodifiableList(theRegisteredElements);
	}
}

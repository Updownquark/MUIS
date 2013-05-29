package org.muis.core.model;

import java.util.List;

import org.muis.core.MuisElement;

public class MuisModelValue<T> implements WidgetRegister {
	private final Class<T> theType;

	private T theValue;

	private List<MuisModelValueListener<? super T>> theListeners;

	private List<MuisElement> theRegisteredElements;

	public MuisModelValue(Class<T> type) {
		theType = type;
		theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		theRegisteredElements = new java.util.concurrent.CopyOnWriteArrayList<>();
	}

	public Class<T> getType() {
		return theType;
	}

	public T get() {
		return theValue;
	}

	public void set(T value, org.muis.core.event.MuisEvent<?> userEvent) {
		T oldValue = theValue;
		theValue = value;
		MuisModelValueEvent<T> event = new MuisModelValueEvent<>(this, userEvent, oldValue, value);
		for(MuisModelValueListener<? super T> listener : theListeners)
			listener.valueChanged(event);
	}

	public void addListener(MuisModelValueListener<? super T> listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	public void removeListener(MuisModelValueListener<? super T> listener) {
		theListeners.remove(listener);
	}

	@Override
	public WidgetRegistration register(final MuisElement widget) {
		if(widget == null)
			return null;
		theRegisteredElements.add(widget);
		return new WidgetRegistration() {
			@Override
			public void unregister() {
				theRegisteredElements.remove(widget);
			}
		};
	}

	@Override
	public List<MuisElement> registered() {
		return java.util.Collections.unmodifiableList(theRegisteredElements);
	}
}

package org.muis.core.model;

import java.util.List;

import org.muis.core.MuisElement;

/**
 * The default implementation of MuisModelValue
 *
 * @param <T> The (compile-time) type of the value
 */
public class DefaultMuisModelValue<T> implements MuisModelValue<T>, WidgetRegister {
	private final Class<T> theType;

	private T theValue;

	private boolean isMutable;

	private List<MuisModelValueListener<? super T>> theListeners;

	private List<MuisElement> theRegisteredElements;

	/** @param type The (run-time) type of the value */
	public DefaultMuisModelValue(Class<T> type) {
		theType = type;
		isMutable = true;
		theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		theRegisteredElements = new java.util.concurrent.CopyOnWriteArrayList<>();
	}

	@Override
	public Class<T> getType() {
		return theType;
	}

	@Override
	public T get() {
		return theValue;
	}

	@Override
	public boolean isMutable() {
		return isMutable;
	}

	/**
	 * @param mutable Whether this value should be modifiable or not
	 * @param userEvent The user event that caused the change. May be null.
	 */
	public void setMutable(boolean mutable, org.muis.core.event.UserEvent userEvent) {
		isMutable = mutable;
		MuisModelValueEvent<T> event = new MuisModelValueEvent<>(this, userEvent, theValue, theValue);
		for(MuisModelValueListener<? super T> listener : theListeners)
			listener.valueChanged(event);
	}

	@Override
	public void set(T value, org.muis.core.event.UserEvent userEvent) {
		if(!isMutable)
			throw new IllegalStateException("This value is not mutable");
		T oldValue = theValue;
		theValue = value;
		MuisModelValueEvent<T> event = new MuisModelValueEvent<>(this, userEvent, oldValue, value);
		for(MuisModelValueListener<? super T> listener : theListeners)
			listener.valueChanged(event);
	}

	@Override
	public void addListener(MuisModelValueListener<? super T> listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	@Override
	public void removeListener(MuisModelValueListener<?> listener) {
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

package org.quick.core.event;


import org.observe.ObservableValueEvent;
import org.quick.core.QuickElement;

import com.google.common.reflect.TypeToken;

/**
 * Represents an event that occurs when a property changes value.
 *
 * @param <T> The type of the property that was changed
 */
public abstract class QuickPropertyEvent<T> extends ObservableValueEvent<T> implements QuickEvent {
	private final QuickElement theElement;

	/**
	 * @param element The element that this event is being fired in
	 * @param initial Whether this represents the population of the initial value of an observable value in response to subscription
	 * @param oldValue The value of the property before it was changed
	 * @param newValue The current value of the property after it has been changed
	 * @param cause The cause of this event
	 */
	public QuickPropertyEvent(QuickElement element, TypeToken<T> type, boolean initial, T oldValue, T newValue, Object cause) {
		super(type, initial, oldValue, newValue, cause);
		theElement = element;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}
}

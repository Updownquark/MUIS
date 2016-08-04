package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;

/**
 * Fired when a style value changes (or for the initial value)
 * 
 * @param <T> The type of the attribute that the value is for
 */
public class StyleAttributeEvent<T> extends ObservableValueEvent<T> {
	/**
	 * @param observable The style value that this event is for
	 * @param initial Whether this is an initial value event, fired in response to {@link ObservableValue#subscribe(org.observe.Observer)}
	 * @param oldValue The previous value of the style
	 * @param newValue The new value of the style
	 * @param cause The event or thing that may have caused this event
	 */
	public StyleAttributeEvent(StyleValue<T> observable, boolean initial, T oldValue, T newValue, Object cause) {
		super(observable, initial, oldValue, newValue, cause);
	}

	@Override
	public StyleValue<T> getObservable() {
		return (StyleValue<T>) super.getObservable();
	}
}

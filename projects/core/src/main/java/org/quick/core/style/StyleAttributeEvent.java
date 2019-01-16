package org.quick.core.style;

import org.observe.Observable;
import org.observe.ObservableValueEvent;

/**
 * Fired when a style value changes (or for the initial value)
 *
 * @param <T> The type of the attribute that the value is for
 */
public class StyleAttributeEvent<T> extends ObservableValueEvent<T> {
	public final StyleAttribute<T> attribute;
	
	/**
	 * @param attribute The style attribute that this event is for
	 * @param initial Whether this is an initial value event, fired in response to {@link Observable#subscribe(org.observe.Observer)}
	 * @param oldValue The previous value of the style
	 * @param newValue The new value of the style
	 * @param cause The event or thing that may have caused this event
	 */
	public StyleAttributeEvent(StyleAttribute<T> attribute, boolean initial, T oldValue, T newValue, Object cause) {
		super(attribute.getType().getType(), initial, oldValue, newValue, cause);
		this.attribute = attribute;
	}
}

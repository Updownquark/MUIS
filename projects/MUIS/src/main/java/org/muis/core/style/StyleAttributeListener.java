package org.muis.core.style;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;

/**
 * A utility listener for listening for changes to a single attribute's value
 * 
 * @param <T> The type of the attribute's value
 */
public abstract class StyleAttributeListener<T> implements MuisEventListener<Void> {
	private final StyleAttribute<T> theAttr;

	/** @param attr The attribute to listen for */
	public StyleAttributeListener(StyleAttribute<T> attr) {
		theAttr = attr;
	}

	@Override
	public void eventOccurred(MuisEvent<Void> event, MuisElement element) {
		if(event instanceof StyleAttributeEvent && theAttr.equals(((StyleAttributeEvent<?>) event).getAttribute())) {
			StyleAttributeEvent<T> styleEvent = (StyleAttributeEvent<T>) event;
			styleChanged(element, styleEvent, styleEvent.getRootStyle().get(theAttr));
		}
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	/**
	 * Called when the target attribute's value changes
	 * 
	 * @param element The element that the change occurred on
	 * @param event The attribute event representing the change
	 * @param value The new value for the attribute
	 */
	public abstract void styleChanged(MuisElement element, StyleAttributeEvent<T> event, T value);
}

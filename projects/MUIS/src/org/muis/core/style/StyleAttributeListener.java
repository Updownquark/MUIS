package org.muis.core.style;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;

public abstract class StyleAttributeListener<T> implements MuisEventListener<Void> {
	private final StyleAttribute<T> theAttr;

	public StyleAttributeListener(StyleAttribute<T> attr) {
		theAttr = attr;
	}

	@Override
	public void eventOccurred(MuisEvent<Void> event, MuisElement element) {
		if(event instanceof StyleAttributeEvent && theAttr.equals(((StyleAttributeEvent<?>) event).getAttribute())) {
			StyleAttributeEvent<T> styleEvent = (StyleAttributeEvent<T>) event;
			styleChanged(element, styleEvent, styleEvent.getStyle().get(theAttr));
		}
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	public abstract void styleChanged(MuisElement element, StyleAttributeEvent<T> event, T value);
}

package org.muis.core.style;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;

/** Listens for changes to style attributes */
public abstract class StyleListener implements org.muis.core.event.MuisEventListener<Void> {
	@Override
	public final void eventOccurred(MuisEvent<Void> event, MuisElement element) {
		if(event instanceof StyleAttributeEvent)
			eventOccurred((StyleAttributeEvent<?>) event);
	}

	/** @param event The event representing the style attribute change */
	public abstract void eventOccurred(StyleAttributeEvent<?> event);
}

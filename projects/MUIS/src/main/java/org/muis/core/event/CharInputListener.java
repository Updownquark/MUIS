package org.muis.core.event;

import org.muis.core.MuisElement;

/** Listens for character input. */
public abstract class CharInputListener implements MuisEventListener<Void> {
	/** Creates a character input listener */
	public CharInputListener() {
	}

	@Override
	public void eventOccurred(MuisEvent<Void> event, MuisElement element) {
		if(event instanceof CharInputEvent) {
			CharInputEvent cEvt = (CharInputEvent) event;
			keyTyped(cEvt, element);
		}
	}

	/**
	 * Called when the user inputs text
	 *
	 * @param evt The key event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public abstract void keyTyped(CharInputEvent evt, MuisElement element);
}

package org.muis.core.event;

import org.muis.core.MuisElement;

/**
 * Listens for {@link KeyBoardEvent}s. This listener is typically inappropriate for text-based listeners such as input boxes and areas. Use
 * {@link CharInputListener} for that purpose.
 */
public abstract class KeyBoardListener implements MuisEventListener<Void> {
	/** Creates a keyboard listener */
	public KeyBoardListener() {
	}

	@Override
	public void eventOccurred(MuisEvent<Void> event, MuisElement element) {
		if(event instanceof KeyBoardEvent) {
			KeyBoardEvent kEvt = (KeyBoardEvent) event;
			if(kEvt.wasPressed())
				keyPressed(kEvt, element);
			else
				keyReleased(kEvt, element);
		}
	}

	/**
	 * Called when the user presses a key
	 *
	 * @param kEvt The key event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void keyPressed(KeyBoardEvent kEvt, MuisElement element) {
	}

	/**
	 * Called when the user releases a key
	 *
	 * @param kEvt The key event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void keyReleased(KeyBoardEvent kEvt, MuisElement element) {
	}
}

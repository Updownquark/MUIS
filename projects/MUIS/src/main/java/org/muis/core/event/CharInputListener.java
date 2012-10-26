package org.muis.core.event;

import org.muis.core.MuisElement;

/** Listens for character input. */
public abstract class CharInputListener implements MuisEventListener<Void>
{
	private boolean isLocal;

	/**
	 * Creates a mouse listener
	 *
	 * @param local Whether the listener should be a local listener, listening only for events that occur on the element, or a subtree
	 *            listener, listening for events that occur on the element's children also
	 */
	public CharInputListener(boolean local)
	{
		isLocal = local;
	}

	@Override
	public void eventOccurred(MuisEvent<Void> event, MuisElement element)
	{
		if(event instanceof CharInputEvent)
		{
			CharInputEvent cEvt = (CharInputEvent) event;
			keyTyped(cEvt, element);
		}
	}

	@Override
	public boolean isLocal()
	{
		return isLocal;
	}

	/**
	 * Called when the user inputs text
	 *
	 * @param evt The key event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void keyTyped(CharInputEvent evt, MuisElement element)
	{
	}
}

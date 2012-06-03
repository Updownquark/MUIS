package org.wam.core.event;

import org.wam.core.WamElement;

/**
 * Listens for character input.
 */
public abstract class CharInputListener implements WamEventListener<Void>
{
	private boolean isLocal;

	/**
	 * Creates a mouse listener
	 * 
	 * @param local Whether the listener should be a local listener, listening only for events that
	 *            occur on the element, or a subtree listener, listening for events that occur on
	 *            the element's children also
	 */
	public CharInputListener(boolean local)
	{
		isLocal = local;
	}

	public void eventOccurred(WamEvent<? extends Void> event, WamElement element)
	{
		if(event instanceof CharInputEvent)
		{
			CharInputEvent cEvt = (CharInputEvent) event;
			keyTyped(cEvt, element);
		}
	}

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
	public void keyTyped(CharInputEvent evt, WamElement element)
	{
	}
}

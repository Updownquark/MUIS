package org.muis.core.event;

import org.muis.core.MuisElement;

/**
 * A listener specifically designed for listening to mouse events. It contains methods that subclasses can override to receive mouse events
 * of specific types:
 * <ul>
 * <li>{@link #mouseDown(MouseEvent, MuisElement)}</li>
 * <li>{@link #mouseUp(MouseEvent, MuisElement)}</li>
 * <li>{@link #mouseClicked(MouseEvent, MuisElement)}</li>
 * <li>{@link #mouseMoved(MouseEvent, MuisElement)}</li>
 * <li>{@link #mouseEntered(MouseEvent, MuisElement)}</li>
 * <li>{@link #mouseExited(MouseEvent, MuisElement)}</li>
 * </ul>
 */
public abstract class MouseListener implements MuisEventListener<Void>
{
	private boolean isLocal;

	/**
	 * Creates a mouse listener
	 *
	 * @param local Whether the listener should be a local listener, listening only for events that occur on the element, or a subtree
	 *            listener, listening for events that occur on the element's children also
	 */
	public MouseListener(boolean local)
	{
		isLocal = local;
	}

	@Override
	public void eventOccurred(MuisEvent<Void> event, MuisElement element)
	{
		if(event instanceof MouseEvent)
		{
			MouseEvent mEvt = (MouseEvent) event;
			boolean switchHit = false;
			switch (mEvt.getMouseEventType())
			{
			case pressed:
				switchHit = true;
				mouseDown(mEvt, element);
				break;
			case released:
				switchHit = true;
				mouseUp(mEvt, element);
				break;
			case clicked:
				switchHit = true;
				mouseClicked(mEvt, element);
				break;
			case moved:
				switchHit = true;
				mouseMoved(mEvt, element);
				break;
			case entered:
				switchHit = true;
				mouseEntered(mEvt, element);
				break;
			case exited:
				switchHit = true;
				mouseExited(mEvt, element);
				break;
			}
			if(!switchHit)
				throw new IllegalStateException("Unrecognized mouse event type: " + mEvt.getMouseEventType());
		}
	}

	@Override
	public boolean isLocal()
	{
		return isLocal;
	}

	/**
	 * Called when the user presses a mouse button over a MUIS element
	 *
	 * @param mEvt The mouse event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void mouseDown(MouseEvent mEvt, MuisElement element)
	{
	}

	/**
	 * Called when the user releases a mouse button over a MUIS element
	 *
	 * @param mEvt The mouse event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void mouseUp(MouseEvent mEvt, MuisElement element)
	{
	}

	/**
	 * Called when the user clicks and releases a mouse button quickly over a MUIS element without moving the mouse pointer
	 *
	 * @param mEvt The mouse event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void mouseClicked(MouseEvent mEvt, MuisElement element)
	{
	}

	/**
	 * Called when the user moves the mouse pointer while over a MUIS element
	 *
	 * @param mEvt The mouse event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void mouseMoved(MouseEvent mEvt, MuisElement element)
	{
	}

	/**
	 * Called when the user moves the mouse pointer from outside a MUIS element to inside it
	 *
	 * @param mEvt The mouse event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void mouseEntered(MouseEvent mEvt, MuisElement element)
	{
	}

	/**
	 * Called when the user moves the mouse pointer from inside a MUIS element to outside it
	 *
	 * @param mEvt The mouse event representing the user's action
	 * @param element The element for which this listener was registered
	 */
	public void mouseExited(MouseEvent mEvt, MuisElement element)
	{
	}
}

package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

/**
 * An event caused by user interaction
 */
public class UserEvent extends MuisEvent<Void>
{
	private final MuisDocument theDocument;

	private final MuisElement theElement;

	private boolean isCanceled;

	/**
	 * Creates a user event
	 * 
	 * @param type The event type that this event is an instance of
	 * @param doc The document that the event occurred in
	 * @param element The deepest-level element that the event occurred in
	 */
	public UserEvent(MuisEventType<Void> type, MuisDocument doc, MuisElement element)
	{
		super(type, null);
		theDocument = doc;
		theElement = element;
	}

	/**
	 * @return The document in which this event occurred
	 */
	public MuisDocument getDocument()
	{
		return theDocument;
	}

	/**
	 * @return The deepest-level element that the event occurred in
	 */
	public MuisElement getElement()
	{
		return theElement;
	}

	/**
	 * @return Whether this event has been canceled
	 */
	public boolean isCanceled()
	{
		return isCanceled;
	}

	/**
	 * Cancels this event so that it will not be propagated to any more elements. This method should
	 * be called when a listener knows what the event was intended for by the user and has the
	 * capability to perform the intended action. In this case, the event should not be propagated
	 * further because its purpose is complete and any further action would be undesirable.
	 */
	public void cancel()
	{
		isCanceled = true;
	}
}

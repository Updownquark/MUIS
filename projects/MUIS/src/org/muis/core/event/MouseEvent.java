package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

/** An event that occurs when the user performs an action with the mouse over a MUIS element */
public class MouseEvent extends PositionedUserEvent
{
	/** Types of buttons that a user may perform events with */
	public static enum ButtonType
	{
		/** Used for an event that was not generated by a button operation */
		NONE,
		/** Represents the left mouse button */
		LEFT,
		/** Represents the right mouse button */
		RIGHT,
		/** Represents the middle mouse button */
		MIDDLE,
		/** Represents an unrecognized mouse button */
		OTHER;
	}

	/** The types of events that can occur as a {@link MouseEvent} */
	public static enum MouseEventType
	{
		/** Represents the user pressing a mouse button */
		pressed,
		/** Represents the user releasing a mouse button */
		released,
		/** Represents the user pressing and releasing a mouse button quickly without moving the mouse pointer */
		clicked,
		/** Represents the user moving the mouse pointer while over an element */
		moved,
		/** Represents the user moving the mouse pointer into an element */
		entered,
		/** Represents the user moving the mouse pointer out of an element */
		exited;
	}

	private final MouseEventType theType;

	private final ButtonType theButtonType;

	private final int theClickCount;

	private final long theTime;

	/**
	 * Creates a mouse event
	 *
	 * @param doc The document that the mouse event occurred in
	 * @param element The deepest-level element that the mouse event occurred over
	 * @param type The type of event that this represents
	 * @param docX The absolute x-coordinate of the event relative to the document's root element
	 * @param docY The absolute y-coordinate of the event relative to the document's root element
	 * @param buttonType The type of the button that caused this mouse event
	 * @param clickCount The number of clicks in quick succession that caused this mouse event
	 */
	public MouseEvent(MuisDocument doc, MuisElement element, MouseEventType type, int docX, int docY, ButtonType buttonType, int clickCount)
	{
		super(MuisElement.MOUSE_EVENT, doc, element, docX, docY);
		theTime = System.currentTimeMillis();
		theType = type;
		theButtonType = buttonType;
		theClickCount = clickCount;
	}

	/** @return The type of this mouse event */
	public MouseEventType getMouseEventType()
	{
		return theType;
	}

	/** @return The button that caused this event */
	public ButtonType getButtonType()
	{
		return theButtonType;
	}

	/** @return The number of clicks in quick succession that caused this event */
	public int getClickCount()
	{
		return theClickCount;
	}

	/** @return The time that this event was created */
	public long getTime()
	{
		return theTime;
	}
}

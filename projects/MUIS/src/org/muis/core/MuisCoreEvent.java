package org.muis.core;

/** Represents an event that the core architecture of MUIS needs to handle */
public class MuisCoreEvent
{
	/** Types of core MUIS events */
	public static enum CoreEventType
	{
		/** Represents a need to repaint all or part of an element */
		paint,
		/** Represents a need to re-layout a container's elements */
		layout,
		/** Represents a need to set the bounds of an element */
		rebound,
		/** Represents a mouse event that needs to be propagated */
		mouse;
	}

	/** The element that the event is for */
	public final MuisElement element;

	/** The area that needs to be repainted in the element, or null for a layout event or to repaint the entire element */
	public final java.awt.Rectangle area;

	/** The type of this event */
	public final CoreEventType type;

	/** The mouse event */
	public org.muis.core.event.MouseEvent mouseEvent;

	/** The time this event was initiated */
	public final long time;

	private volatile boolean isActed;

	private volatile boolean isDone;

	/**
	 * @param el The element that the event is for
	 * @param aType The type of the event
	 */
	public MuisCoreEvent(MuisElement el, CoreEventType aType)
	{
		this(el, aType, null);
	}

	/**
	 * @param el The element that the event is for
	 * @param aType The type of the event
	 * @param anArea The area for the event. May be null.
	 */
	public MuisCoreEvent(MuisElement el, CoreEventType aType, java.awt.Rectangle anArea)
	{
		element = el;
		type = aType;
		time = System.currentTimeMillis();
		area = anArea;
	}

	/** @param evt The mouse event that this event is for */
	public MuisCoreEvent(org.muis.core.event.MouseEvent evt)
	{
		element = evt.getElement();
		type = CoreEventType.mouse;
		time = evt.getTime();
		area = null;
		mouseEvent = evt;
	}

	/** Called by the event queue when it begins taking action on this event */
	void act()
	{
		isActed = true;
	}

	/** Called by the event queue when this event's action is finished or when the event is deemed irrelevant */
	void done()
	{
		isDone = true;
	}

	/** @return Whether this event's action is being acted upon or has been acted upon */
	public boolean isActed()
	{
		return isActed;
	}

	/** @return Whether this event's action has finished or the event has been deemed irrelevant */
	public boolean isDone()
	{
		return isDone;
	}
}

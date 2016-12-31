package org.quick.core.event;

import java.util.Set;

import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;
import org.quick.core.QuickEventPositionCapture;

/** An event that occurs when the user performs an action with the mouse over a Quick element */
public class MouseEvent extends PositionedUserEvent {
	/** Filters mouse events that have not been {@link UserEvent#use() used} */
	public static final MouseEventCondition mouse = MouseEventCondition.mouse;

	/** Types of buttons that a user may perform events with */
	public static enum ButtonType {
		/** Used for an event that was not generated by a button operation */
		none,
		/** Represents the left mouse button */
		left,
		/** Represents the right mouse button */
		right,
		/** Represents the middle mouse button */
		middle,
		/** Represents an unrecognized mouse button */
		other;
	}

	/** The types of events that can occur as a {@link MouseEvent} */
	public static enum MouseEventType {
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

	private final MouseEvent theBacking;

	private final MouseEventType theType;

	private final ButtonType theButton;

	private final int theClickCount;

	/**
	 * Creates a mouse event
	 *
	 * @param doc The document that the mouse event occurred in
	 * @param target The deepest-level element that the mouse event occurred over
	 * @param type The type of event that this represents
	 * @param button The mouse button that caused this mouse event
	 * @param clickCount The number of clicks in quick succession that caused this mouse event
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param capture The capture of the event's location on each element relevant to it
	 */
	public MouseEvent(QuickDocument doc, QuickElement target, MouseEventType type, ButtonType button, int clickCount,
		Set<MouseEvent.ButtonType> pressedButtons, Set<KeyBoardEvent.KeyCode> pressedKeys, QuickEventPositionCapture capture) {
		super(doc, target, target, pressedButtons, pressedKeys, System.currentTimeMillis(), capture);
		if(capture == null)
			throw new IllegalStateException("MouseEvent cannot be instantiated without a capture");
		theBacking = null;
		theType = type;
		theButton = button;
		theClickCount = clickCount;
	}

	private MouseEvent(MouseEvent backing, QuickEventPositionCapture capture) {
		super(backing.getDocument(), backing.getTarget(), capture.getElement(), backing.getPressedButtons(), backing.getPressedKeys(),
			backing.getTime(), capture);
		theBacking = backing;
		theType = backing.theType;
		theButton = backing.theButton;
		theClickCount = backing.theClickCount;
	}

	/** @return The type of this mouse event */
	public MouseEventType getType() {
		return theType;
	}

	/** @return The mouse button that caused this event */
	public ButtonType getButton() {
		return theButton;
	}

	/** @return The number of clicks in quick succession that caused this event */
	public int getClickCount() {
		return theClickCount;
	}

	@Override
	public QuickEvent getCause() {
		return null;
	}

	@Override
	public MouseEvent copyFor(QuickElement element) {
		QuickEventPositionCapture capture = getCapture().find(element);
		if(capture == null)
			throw new IllegalArgumentException("This event (" + this + ") is not relevant to the given element (" + element + ")");
		return new MouseEvent(this, capture);
	}

	@Override
	public boolean isUsed() {
		if(theBacking != null)
			return theBacking.isUsed();
		else
			return super.isUsed();
	}

	@Override
	public void use() {
		if(theBacking != null)
			theBacking.use();
		else
			super.use();
	}

	@Override
	public String toString() {
		return "Mouse " + theType + " with " + theButton + (theClickCount > 1 ? " x" + theClickCount : "") + " at " + getElement();
	}
}

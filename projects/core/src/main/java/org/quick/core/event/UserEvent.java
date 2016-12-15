package org.quick.core.event;

import java.util.List;

import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;

/** An event caused by user interaction */
public abstract class UserEvent implements QuickEvent {
	private final QuickDocument theDocument;

	private final QuickElement theTarget;

	private final QuickElement theElement;

	private final List<MouseEvent.ButtonType> thePressedButtons;
	private final List<KeyBoardEvent.KeyCode> thePressedKeys;

	private final long theTime;

	private boolean isUsed;

	/**
	 * Creates a user event
	 *
	 * @param doc The document that the event occurred in
	 * @param target The deepest-level element that the event occurred in
	 * @param element The element that this event is being fired on
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param time The time at which user performed this action
	 */
	public UserEvent(QuickDocument doc, QuickElement target, QuickElement element, List<MouseEvent.ButtonType> pressedButtons,
		List<KeyBoardEvent.KeyCode> pressedKeys, long time) {
		theDocument = doc;
		theTarget = element;
		theElement = element;
		thePressedButtons = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(pressedButtons));
		thePressedKeys = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(pressedKeys));
		theTime = time;
	}

	/** @return The document in which this event occurred */
	public QuickDocument getDocument() {
		return theDocument;
	}

	/** @return The deepest-level element that the event occurred in */
	public QuickElement getTarget() {
		return theTarget;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	/** @return Whether a shift button was pressed when this event was generated */
	public boolean isShiftPressed() {
		return thePressedKeys.contains(KeyBoardEvent.KeyCode.SHIFT_LEFT) || thePressedKeys.contains(KeyBoardEvent.KeyCode.SHIFT_RIGHT);
	}

	/** @return Whether a control button was pressed when this event was generated */
	public boolean isControlPressed() {
		return thePressedKeys.contains(KeyBoardEvent.KeyCode.CTRL_LEFT) || thePressedKeys.contains(KeyBoardEvent.KeyCode.CTRL_RIGHT);
	}

	/** @return Whether an alt button was pressed when this event was generated */
	public boolean isAltPressed() {
		return thePressedKeys.contains(KeyBoardEvent.KeyCode.ALT_LEFT) || thePressedKeys.contains(KeyBoardEvent.KeyCode.ALT_RIGHT);
	}

	/** @return The mouse buttons which were pressed when this event was generated */
	public List<MouseEvent.ButtonType> getPressedButtons() {
		return thePressedButtons;
	}

	/** @return The keyboard key which were pressed when this event was generated */
	public List<KeyBoardEvent.KeyCode> getPressedKeys() {
		return thePressedKeys;
	}

	/** @return The time at which the user performed this action */
	public long getTime() {
		return theTime;
	}

	/** @return Whether this event has been acted upon */
	public boolean isUsed() {
		return isUsed;
	}

	/**
	 * Marks this event as used so that it will not be acted upon by any more elements. This method should be called when a listener knows
	 * what the event was intended for by the user and has the capability to perform the intended action. In this case, the event should not
	 * be acted upon elsewhere because its purpose is complete and any further action would be undesirable.
	 */
	public void use() {
		isUsed = true;
	}

	/**
	 * @param element The element to return an event relative to
	 * @return This event relative to the given element. The given element will not be the element returned by {@link #getElement()}, but
	 *         the positions returned by this event's methods will be relative to the given element's position. The event's stateful
	 *         properties (e.g. {@link UserEvent#isUsed() used}) will be backed by this event's properties.
	 */
	public abstract UserEvent copyFor(QuickElement element);

	@Override
	public boolean isOverridden() {
		return false;
	}
}

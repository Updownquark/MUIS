package org.quick.widget.core.event;

import java.util.Set;

import org.qommons.Causable;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.event.KeyBoardEvent.KeyCode;
import org.quick.widget.core.event.MouseEvent.ButtonType;

/** An event caused by user interaction */
public abstract class UserEvent extends Causable implements QuickWidgetEvent {
	private final QuickWidgetDocument theDocument;

	private final QuickWidget theTarget;

	private final QuickWidget theWidget;

	private final Set<MouseEvent.ButtonType> thePressedButtons;
	private final Set<KeyBoardEvent.KeyCode> thePressedKeys;

	private final long theTime;

	private boolean isUsed;

	/**
	 * Creates a user event
	 *
	 * @param doc The document that the event occurred in
	 * @param target The deepest-level widget that the event occurred in
	 * @param widget The widget that this event is being fired on
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param time The time at which user performed this action
	 * @param cause The cause of this event
	 */
	public UserEvent(QuickWidgetDocument doc, QuickWidget target, QuickWidget widget, Set<ButtonType> pressedButtons,
		Set<KeyCode> pressedKeys, long time, Object cause) {
		super(cause);
		theDocument = doc;
		theTarget = widget;
		theWidget = widget;
		thePressedButtons = java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(pressedButtons));
		thePressedKeys = java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(pressedKeys));
		theTime = time;
	}

	/** @return The document in which this event occurred */
	public QuickWidgetDocument getDocument() {
		return theDocument;
	}

	/** @return The deepest-level element that the event occurred in */
	public QuickWidget getTarget() {
		return theTarget;
	}

	@Override
	public QuickWidget getWidget() {
		return theWidget;
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
	public Set<MouseEvent.ButtonType> getPressedButtons() {
		return thePressedButtons;
	}

	/** @return The keyboard key which were pressed when this event was generated */
	public Set<KeyBoardEvent.KeyCode> getPressedKeys() {
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
	 * @param widget The widgetto return an event relative to
	 * @return This event relative to the given widget. The given element will not be the element returned by {@link #getWidget()}, but the
	 *         positions returned by this event's methods will be relative to the given element's position. The event's stateful properties
	 *         (e.g. {@link UserEvent#isUsed() used}) will be backed by this event's properties.
	 */
	public abstract UserEvent copyFor(QuickWidget widget);
}

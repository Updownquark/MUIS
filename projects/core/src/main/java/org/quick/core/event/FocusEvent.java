package org.quick.core.event;

import java.util.List;

import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;

/** An event that occurs when the user changes focus */
public class FocusEvent extends UserEvent {
	/** Filters focus events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focusEvent = FocusEventCondition.focusEvent;

	/** Filters focus gained events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focus = FocusEventCondition.focus;

	/** Filters focus lost events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition blur = FocusEventCondition.blur;

	private final UserEvent theCause;
	private final FocusEvent theBacking;

	private final boolean isFocus;

	/**
	 * Creates a FocusEvent
	 *
	 * @param doc The document that the event was fired on
	 * @param target The focused element that received this key event first
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param gained Whether the element gained the focus (true) or lost the focus (false)
	 * @param cause The event that caused this event
	 */
	public FocusEvent(QuickDocument doc, QuickElement target, List<MouseEvent.ButtonType> pressedButtons,
		List<KeyBoardEvent.KeyCode> pressedKeys, boolean gained, UserEvent cause) {
		super(doc, target, target, pressedButtons, pressedKeys, System.currentTimeMillis());
		theCause = cause;
		theBacking = null;
		isFocus = gained;
	}

	private FocusEvent(FocusEvent backing, QuickElement element) {
		super(backing.getDocument(), backing.getTarget(), element, backing.getPressedButtons(), backing.getPressedKeys(), backing.getTime());
		theCause = backing.getCause();
		theBacking = backing;
		isFocus = backing.isFocus;
	}

	@Override
	public UserEvent getCause() {
		return theCause;
	}

	/** @return Whether this event represents the element coming into focus (true) or out of focus (false) */
	public boolean isFocus() {
		return isFocus;
	}

	@Override
	public FocusEvent copyFor(QuickElement element) {
		if(!org.quick.util.QuickUtils.isAncestor(element, getTarget()))
			throw new IllegalArgumentException("This event (" + this + ") is not relevant to the given element (" + element + ")");
		return new FocusEvent(this, element);
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
		return "Focus event: " + isFocus + " at " + getElement();
	}
}

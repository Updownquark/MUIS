package org.quick.widget.core.event;

import java.util.Set;

import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

/** An event that occurs when the user changes focus */
public class FocusEvent extends UserEvent {
	/** Filters focus events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focusEvent = FocusEventCondition.focusEvent;

	/** Filters focus gained events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition focus = FocusEventCondition.focus;

	/** Filters focus lost events that have not been {@link UserEvent#use() used} */
	public static final FocusEventCondition blur = FocusEventCondition.blur;

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
	public FocusEvent(QuickWidgetDocument doc, QuickWidget target, Set<MouseEvent.ButtonType> pressedButtons,
		Set<KeyBoardEvent.KeyCode> pressedKeys, boolean gained, UserEvent cause) {
		super(doc, target, target, pressedButtons, pressedKeys, System.currentTimeMillis(), cause);
		theBacking = null;
		isFocus = gained;
	}

	private FocusEvent(FocusEvent backing, QuickWidget widget) {
		super(backing.getDocument(), backing.getTarget(), widget, backing.getPressedButtons(), backing.getPressedKeys(), backing.getTime(),
			backing.getCause());
		theBacking = backing;
		isFocus = backing.isFocus;
	}

	@Override
	public UserEvent getCause() {
		return (UserEvent) super.getCause();
	}

	/** @return Whether this event represents the element coming into focus (true) or out of focus (false) */
	public boolean isFocus() {
		return isFocus;
	}

	@Override
	public FocusEvent copyFor(QuickWidget widget) {
		if (!org.quick.util.QuickUtils.isAncestor(widget.getElement(), getTarget().getElement()))
			throw new IllegalArgumentException("This event (" + this + ") is not relevant to the given element (" + widget + ")");
		return new FocusEvent(this, widget);
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
		return "Focus event: " + isFocus + " at " + getWidget();
	}
}

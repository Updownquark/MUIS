package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

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
	 * @param gained Whether the element gained the focus (true) or lost the focus (false)
	 */
	public FocusEvent(MuisDocument doc, MuisElement target, boolean gained) {
		super(doc, target, target, System.currentTimeMillis());
		theBacking = null;
		isFocus = gained;
	}

	private FocusEvent(FocusEvent backing, MuisElement element) {
		super(backing.getDocument(), backing.getTarget(), element, backing.getTime());
		theBacking = backing;
		isFocus = backing.isFocus;
	}

	/** @return Whether this event represents the element coming into focus (true) or out of focus (false) */
	public boolean isFocus() {
		return isFocus;
	}

	@Override
	public FocusEvent copyFor(MuisElement element) {
		if(!org.muis.util.MuisUtils.isAncestor(element, getTarget()))
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

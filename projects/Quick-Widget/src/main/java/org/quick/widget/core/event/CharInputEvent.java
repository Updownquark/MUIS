package org.quick.widget.core.event;

import java.util.Set;

import org.quick.core.event.QuickEvent;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;

/** This event represents a character of textual input, typically as a result of a keystroke or combination of keystrokes */
public class CharInputEvent extends UserEvent {
	/** Filters events of this type */
	public static final java.util.function.Function<QuickWidgetEvent, CharInputEvent> charInput = value -> {
		return value instanceof CharInputEvent ? (CharInputEvent) value : null;
	};

	/** The control character representing a paste event (typically from the user pressing Ctrl+V) */
	public static final char PASTE = 22;

	private final CharInputEvent theBacking;

	private char theChar;

	/**
	 * Creates a CharInputEvent
	 *
	 * @param doc The document that the event occurred in
	 * @param target The deepest-level widget that the event was directed to (the focus)
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param character The character that was input
	 */
	public CharInputEvent(QuickWidgetDocument doc, QuickWidget target, Set<MouseEvent.ButtonType> pressedButtons,
		Set<KeyBoardEvent.KeyCode> pressedKeys, char character, Object cause) {
		super(doc, target, target, pressedButtons, pressedKeys, System.currentTimeMillis(), cause);
		theBacking = null;
		theChar = character;
	}

	private CharInputEvent(CharInputEvent backing, QuickWidget element) {
		super(backing.getDocument(), backing.getTarget(), element, backing.getPressedButtons(), backing.getPressedKeys(), backing.getTime(),
			backing.getCause());
		theBacking = backing;
		theChar = backing.getChar();
	}

	/** @return The character that was input to generate this event */
	public char getChar() {
		return theChar;
	}

	@Override
	public QuickEvent getCause() {
		return null;
	}

	@Override
	public CharInputEvent copyFor(QuickWidget widget) {
		if (!org.quick.util.QuickUtils.isAncestor(widget.getElement(), getTarget().getElement()))
			throw new IllegalArgumentException("This event (" + this + ") is not relevant to the given widget (" + widget + ")");
		return new CharInputEvent(this, widget);
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
		return "Character input: " + theChar + " at " + getWidget();
	}
}

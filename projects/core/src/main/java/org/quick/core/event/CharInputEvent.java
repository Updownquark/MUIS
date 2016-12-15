package org.quick.core.event;

import java.util.List;

import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;

/** This event represents a character of textual input, typically as a result of a keystroke or combination of keystrokes */
public class CharInputEvent extends UserEvent {
	/** Filters events of this type */
	public static final java.util.function.Function<QuickEvent, CharInputEvent> charInput = value -> {
		return value instanceof CharInputEvent ? (CharInputEvent) value : null;
	};

	/** The control character representing a paste event (typically from the user pressing Ctrl+V) */
	public static final char PASTE = 22;

	private final CharInputEvent theBacking;

	private char theChar;

	/**
	 * Creates a CharInputEvent
	 *
	 * @param doc The QuickDocument that the event occurred in
	 * @param target The deepest-level element that the event was directed to (the focus)
	 * @param pressedButtons The mouse buttons which were pressed when this event was generated
	 * @param pressedKeys The keyboard keys which were pressed when this event was generated
	 * @param character The character that was input
	 */
	public CharInputEvent(QuickDocument doc, QuickElement target, List<MouseEvent.ButtonType> pressedButtons,
		List<KeyBoardEvent.KeyCode> pressedKeys, char character) {
		super(doc, target, target, pressedButtons, pressedKeys, System.currentTimeMillis());
		theBacking = null;
		theChar = character;
	}

	private CharInputEvent(CharInputEvent backing, QuickElement element) {
		super(backing.getDocument(), backing.getTarget(), element, backing.getPressedButtons(), backing.getPressedKeys(), backing.getTime());
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
	public CharInputEvent copyFor(QuickElement element) {
		if(!org.quick.util.QuickUtils.isAncestor(element, getTarget()))
			throw new IllegalArgumentException("This event (" + this + ") is not relevant to the given element (" + element + ")");
		return new CharInputEvent(this, element);
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
		return "Character input: " + theChar + " at " + getElement();
	}
}

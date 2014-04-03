package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

/** This event represents a character of textual input, typically as a result of a keystroke or combination of keystrokes */
public class CharInputEvent extends UserEvent {
	/** The control character representing a paste event (typically from the user pressing Ctrl+V) */
	public static final char PASTE = 22;

	private final CharInputEvent theBacking;

	private char theChar;

	/**
	 * Creates a CharInputEvent
	 *
	 * @param doc The MuisDocument that the event occurred in
	 * @param target The deepest-level element that the event was directed to (the focus)
	 * @param character The character that was input
	 */
	public CharInputEvent(MuisDocument doc, MuisElement target, char character) {
		super(doc, target, target, System.currentTimeMillis());
		theBacking = null;
		theChar = character;
	}

	private CharInputEvent(CharInputEvent backing, MuisElement element) {
		super(backing.getDocument(), backing.getTarget(), element, backing.getTime());
		theBacking = backing;
		theChar = backing.getChar();
	}

	/** @return The character that was input to generate this event */
	public char getChar() {
		return theChar;
	}

	@Override
	public CharInputEvent copyFor(MuisElement element) {
		if(!org.muis.util.MuisUtils.isAncestor(element, getTarget()))
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

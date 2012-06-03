package org.wam.core.event;

import org.wam.core.WamDocument;
import org.wam.core.WamElement;

/**
 * This event represents a character of textual input, typically as a result of a keystroke or
 * combination of keystrokes
 */
public class CharInputEvent extends UserEvent
{
	private char theChar;

	/**
	 * Creates a CharInputEvent
	 * 
	 * @param doc The WamDocument that the event occurred in
	 * @param element The WamElement that the event was directed to (the focus)
	 * @param character The character that was input
	 */
	public CharInputEvent(WamDocument doc, WamElement element, char character)
	{
		super(WamElement.CHARACTER_INPUT, doc, element);
		theChar = character;
	}

	/**
	 * @return The character that was input to generate this event
	 */
	public char getChar()
	{
		return theChar;
	}
}

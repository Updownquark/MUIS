package org.muis.core.event;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

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
	 * @param doc The MuisDocument that the event occurred in
	 * @param element The MuisElement that the event was directed to (the focus)
	 * @param character The character that was input
	 */
	public CharInputEvent(MuisDocument doc, MuisElement element, char character)
	{
		super(org.muis.core.MuisConstants.Events.CHARACTER_INPUT, doc, element);
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

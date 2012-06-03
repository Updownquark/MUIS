package org.muis.core;

/**
 * An implementation of this class may be specified (as namespace:tagname) in an attach[0-9]*
 * attributes so that the parser will call the {@link #attach(MuisElement)} method, allowing user
 * code to run on the element.
 */
public interface MuisElementAttachment
{
	/**
	 * Called by the parser to allow an implementation to perform operations on the element, such as
	 * adding listeners, adding content, etc.
	 * 
	 * @param element The element that the attach attribute was specified in
	 */
	void attach(MuisElement element);
}

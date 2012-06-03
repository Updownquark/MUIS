package org.muis.core;

/**
 * Marks a MUIS element as a container, simply exposing the addChild and removeChild methods publicly
 */
public interface MuisContainer
{
	/**
	 * @see MuisElement#addChild(MuisElement, int)
	 * 
	 * @param child The child to add
	 * @param index The index to add the child at, or -1 to add it as the last child
	 */
	void addChild(MuisElement child, int index);

	/**
	 * @see MuisElement#removeChild(int)
	 * 
	 * @param index The indedx of the child to remove
	 * @return The child that was removed
	 */
	MuisElement removeChild(int index);
}

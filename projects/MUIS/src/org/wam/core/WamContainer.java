package org.wam.core;

/**
 * Marks a WAM element as a container, simply exposing the addChild and removeChild methods publicly
 */
public interface WamContainer
{
	/**
	 * @see WamElement#addChild(WamElement, int)
	 * 
	 * @param child The child to add
	 * @param index The index to add the child at, or -1 to add it as the last child
	 */
	void addChild(WamElement child, int index);

	/**
	 * @see WamElement#removeChild(int)
	 * 
	 * @param index The indedx of the child to remove
	 * @return The child that was removed
	 */
	WamElement removeChild(int index);
}

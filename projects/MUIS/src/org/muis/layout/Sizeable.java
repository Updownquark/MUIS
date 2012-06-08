package org.muis.layout;

public interface Sizeable
{
	/**
	 * @param height The height value to get the width policy for, or -1 to get the absolute min, max, and preferred width for the item
	 * @return The size policy for this element's width
	 */
	SizePolicy getWSizer(int height);

	/**
	 * @param width The width value to get the height policy for, or -1 to get the absolute min, max, and preferred height for the item
	 * @return The size policy for this element's height
	 */
	SizePolicy getHSizer(int width);
}

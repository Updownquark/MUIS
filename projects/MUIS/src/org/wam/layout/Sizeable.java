package org.wam.layout;

public interface Sizeable
{
	/**
	 * @param height The height value to get the horizontal (width) policy for, or -1 to get the absolute min, max, and
	 *        preferred width for the item
	 * @return The size policy for this element's width
	 */
	SizePolicy getHSizer(int height);

	/**
	 * @param width The width value to get the vertical (height) policy for, or -1 to get the absolute min, max, and
	 *        preferred height for the item
	 * @return The size policy for this element's height
	 */
	SizePolicy getVSizer(int width);
}

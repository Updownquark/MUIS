package org.muis.core.layout;

/** Used by layouts to determine what size a widget would like to be in a single dimension */
public interface SizePolicy {
	/** @return The minimum dimension that the widget can take independent of the crossways dimension and be happy */
	int getMinPreferred();

	/** @return The maximum dimension that the widget can take independent of the crossways dimension and be happy */
	int getMaxPreferred();
	
	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @return The minimum size that the widget can allow. Good layouts should never size a widget smaller than this
	 *         value.
	 */
	int getMin(int crossSize);

	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @return The optimum size for the widget, or possibly the minimum good size. This value is the size below which
	 *         the widget becomes cluttered or has to shrink its content more than is desirable. A preferred size of <=0
	 *         means the widget has no size preference.
	 */
	int getPreferred(int crossSize);

	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @return The maximum size for the widget. Good layouts should never size a widget larger than this value
	 */
	int getMax(int crossSize);

	/**
	 * @return The stretch factor for the widget, between 0 and 1. This determines how layouts will distribute extra
	 *         space if there is leftover space after all content has been given its preferred size. Larger values mean
	 *         the widget can make better use of extra space than other widgets. A value of 0 means that the widget may
	 *         be given extra space (up to its max size) but with no advantage at all. A value of 1 means the widget is
	 *         eager to stretch out to its maximum.
	 */
	float getStretch();
}

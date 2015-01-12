package org.muis.core.layout;

/** Used by layouts to determine what size a widget would like to be in a single dimension */
public interface SizeGuide {
	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @param csMax Whether the cross size should be treated as a maximum or an absolute size
	 * @return The minimum size that the widget can allow. Good layouts should never size a widget smaller than this value.
	 */
	int getMin(int crossSize, boolean csMax);

	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @param csMax Whether the cross size should be treated as a maximum or an absolute size
	 * @return The minimum dimension that the widget can take and be happy
	 */
	int getMinPreferred(int crossSize, boolean csMax);

	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @param csMax Whether the cross size should be treated as a maximum or an absolute size
	 * @return The optimum size for the widget, or possibly the minimum good size. This value is the size below which the widget becomes
	 *         cluttered or has to shrink its content more than is desirable. A preferred size of <=0 means the widget has no size
	 *         preference.
	 */
	int getPreferred(int crossSize, boolean csMax);

	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @param csMax Whether the cross size should be treated as a maximum or an absolute size
	 * @return The maximum dimension that the widget can make use of
	 */
	int getMaxPreferred(int crossSize, boolean csMax);

	/**
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @param csMax Whether the cross size should be treated as a maximum or an absolute size
	 * @return The maximum size for the widget. Good layouts should never size a widget larger than this value
	 */
	int getMax(int crossSize, boolean csMax);

	/**
	 * @param type The layout guide type to get the size guide setting for
	 * @param crossSize The hypothetical size of the widget in the opposite dimension
	 * @param csMax Whether the cross size should be treated as a maximum or an absolute size
	 * @return The size guide setting of the given type in this guide
	 */
	int get(LayoutGuideType type, int crossSize, boolean csMax);

	/**
	 * @param size The hypothetical size of the widget in this dimension
	 * @return The location of the baseline in this dimension
	 */
	int getBaseline(int size);

	/**
	 * @param size The size to check
	 * @param crossSize The size of the widget in the opposite dimension
	 * @return 0 if the given size is within this guide's preferred range, -1 if the size is below minPreferred, 1 if the size is above
	 *         maxPreferred
	 */
	default int compareToPreferred(int size, int crossSize) {
		if(size < getMinPreferred(crossSize, false))
			return -1;
		else if(size <= getMaxPreferred(crossSize, false))
			return 0;
		else
			return 1;
	}

	/**
	 * @param size The size to check
	 * @param crossSize The size of the widget in the opposite dimension
	 * @return 0 if the given size is within this guide's extremity range, -1 if the size is below min, 1 if the size is above max
	 */
	default int compareToMinMax(int size, int crossSize) {
		if(size < getMin(crossSize, false))
			return -1;
		else if(size <= getMax(crossSize, false))
			return 0;
		else
			return 1;
	}

	/**
	 * A utility for adding 2 numbers, the sum of which may be greater than an integer's capacity
	 *
	 * @param i1 The first number to add
	 * @param i2 The second number to add
	 * @return The added result, or {@link Integer#MAX_VALUE} if the result is too great for the capacity of an integer
	 */
	public static int add(int i1, int i2) {
		int ret = i1 + i2;
		if(ret < i1)
			return Integer.MAX_VALUE;
		return ret;
	}
}

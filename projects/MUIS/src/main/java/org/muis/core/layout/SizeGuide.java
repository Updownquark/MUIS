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
}

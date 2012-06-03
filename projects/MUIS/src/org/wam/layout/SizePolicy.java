package org.wam.layout;

/** Used by layouts to determine what size a widget would like to be */
public interface SizePolicy
{
	/**
	 * @return The minimum size that the widget can allow. Good layouts should never size a widget smaller than this
	 *         value.
	 */
	int getMin();

	/**
	 * @return The optimum size for the widget, or possibly the minimum good size. This value is the size below which
	 *         the widget becomes cluttered or has to shrink its content more than is desirable. A preferred size of <=0
	 *         means the widget has no size preference.
	 */
	int getPreferred();

	/**
	 * @return The maximum size for the widget. Good layouts should never size a widget larger than this value
	 */
	int getMax();

	/**
	 * @return The stretch factor for the widget. This determines how layouts will distribute extra space if there is
	 *         leftover space after all content has been given its preferred size. Larger values mean the widget can
	 *         make better use of extra space than other widgets. A <=0 value means that the widget may be given extra
	 *         space (depending on its max size) but with no advantage.
	 */
	int getStretch();
}

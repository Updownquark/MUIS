package org.quick.core.layout;

/** Used by layouts to determine what size a widget would like to be in a single dimension */
public interface SizeGuide {
	/**
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossSize Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param csAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the widget
	 *        in the opposite dimension
	 * @return The minimum size that the widget can allow. Good layouts should never size a widget smaller than this value.
	 */
	default int getMin(int layoutLength, int crossSize, boolean csAvailable) {
		return get(LayoutGuideType.min, layoutLength, crossSize, csAvailable);
	}

	/**
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossSize Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param csAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the widget
	 *        in the opposite dimension
	 * @return The minimum dimension that the widget can take and be happy
	 */
	default int getMinPreferred(int layoutLength, int crossSize, boolean csAvailable) {
		return get(LayoutGuideType.minPref, layoutLength, crossSize, csAvailable);
	}

	/**
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossSize Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param csAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the widget
	 *        in the opposite dimension
	 * @return The optimum size for the widget, or possibly the minimum good size. This value is the size below which the widget becomes
	 *         cluttered or has to shrink its content more than is desirable. A preferred size of <=0 means the widget has no size
	 *         preference.
	 */
	default int getPreferred(int layoutLength, int crossSize, boolean csAvailable) {
		return get(LayoutGuideType.pref, layoutLength, crossSize, csAvailable);
	}

	/**
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossSize Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param csAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the widget
	 *        in the opposite dimension
	 * @return The maximum dimension that the widget can make use of
	 */
	default int getMaxPreferred(int layoutLength, int crossSize, boolean csAvailable) {
		return get(LayoutGuideType.maxPref, layoutLength, crossSize, csAvailable);
	}

	/**
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossSize Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param csAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the widget
	 *        in the opposite dimension
	 * @return The maximum size for the widget. Good layouts should never size a widget larger than this value
	 */
	default int getMax(int layoutLength, int crossSize, boolean csAvailable) {
		return get(LayoutGuideType.max, layoutLength, crossSize, csAvailable);
	}

	/**
	 * @param type The layout guide type to get the size guide setting for
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossLength Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param crossAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the
	 *        widget in the opposite dimension
	 * @return The size guide setting of the given type in this guide
	 */
	int get(LayoutGuideType type, int layoutLength, int crossLength, boolean crossAvailable);

	/**
	 * A default method that calls one of:
	 * <ol>
	 * <li>{@link #getMin(int, int, boolean)}</li>
	 * <li>{@link #getMinPreferred(int, int, boolean)}</li>
	 * <li>{@link #getPreferred(int, int, boolean)}</li>
	 * <li>{@link #getMaxPreferred(int, int, boolean)}</li>
	 * <li>{@link #getMax(int, int, boolean)}</li>
	 * </ol>
	 *
	 * @param type The layout guide type to get the size guide setting for
	 * @param layoutLength The size of the layout space that this guide is in
	 * @param crossLength Either the size of the widget or the amount of space available to the widget in the opposite dimension
	 * @param crossAvailable Whether <code>crossSize</code> is to be interpreted as available size in the layout or absolute size of the
	 *        widget in the opposite dimension
	 * @return The size guide setting of the given type in this guide
	 */
	default int delegate(LayoutGuideType type, int layoutLength, int crossLength, boolean crossAvailable) {
		switch (type) {
		case min:
			return getMin(layoutLength, crossLength, crossAvailable);
		case minPref:
			return getMinPreferred(layoutLength, crossLength, crossAvailable);
		case pref:
			return getPreferred(layoutLength, crossLength, crossAvailable);
		case maxPref:
			return getMaxPreferred(layoutLength, crossLength, crossAvailable);
		case max:
			return getMax(layoutLength, crossLength, crossAvailable);
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + type);
	}

	/**
	 * @param layoutLength The hypothetical size of the widget in this dimension
	 * @return The location of the baseline in this dimension
	 */
	int getBaseline(int layoutLength);
}

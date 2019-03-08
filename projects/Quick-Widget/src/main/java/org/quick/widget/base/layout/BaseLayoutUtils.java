package org.quick.widget.base.layout;

import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.LayoutSize;
import org.quick.core.layout.Orientation;
import org.quick.core.style.Size;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.layout.LayoutUtils;

/** Utilities for use by layouts in Quick base */
public class BaseLayoutUtils {
	/**
	 * Gets the length of a layout of a set of widgets along a common axis
	 *
	 * @param children The children to lay out
	 * @param orient The orientation of the layout
	 * @param type The type of size to get
	 * @param crossSize The size of the container opposite to the layout's axis
	 * @param csMax Whether the cross size is to be considered a maximum
	 * @param paddingX The padding size along the horizontal axis
	 * @param paddingY The padding size along the vertical axis
	 * @return The main size of the layout
	 */
	public static int getBoxLayoutSize(Iterable<? extends QuickWidget> children, Orientation orient, LayoutGuideType type, int crossSize,
		boolean csMax,
		Size paddingX, Size paddingY) {
		LayoutSize temp = new LayoutSize();
		boolean first = true;
		for (QuickWidget child : children) {
			if (first && type != LayoutGuideType.min) {
				first = false;
				temp.add(orient == Orientation.horizontal ? paddingX : paddingY);
			}
			LayoutUtils.getSize(child, orient, type, Integer.MAX_VALUE, crossSize, csMax, temp);
		}
		if(temp.getPixels() == 0) {
			switch (type) {
			case min:
			case minPref:
			case pref:
				return 0;
			case max:
			case maxPref:
				return Integer.MAX_VALUE;
			}
		}
		return temp.getTotal();
	}

	/**
	 * Gets the breadth of a set of widgets opposite to a common layout axis
	 *
	 * @param children The children to lay out
	 * @param orient The orientation of the layout
	 * @param type The type of size to get
	 * @param mainSize The size of the container along the layout's axis
	 * @param sizeMax Whether the main size is to be considered a maximum
	 * @param addTo The layout size to add the result to (may be null)
	 * @return The cross size of the layout
	 */
	public static int getBoxLayoutCrossSize(Iterable<? extends QuickWidget> children, Orientation orient, LayoutGuideType type,
		int mainSize,
		boolean sizeMax, LayoutSize addTo) {
		LayoutSize temp = new LayoutSize(true);
		int ret = 0;
		for (QuickWidget child : children) {
			int sz = LayoutUtils.getSize(child, orient.opposite(), type, Integer.MAX_VALUE, mainSize, sizeMax, temp);
			if(sz > ret)
				ret = sz;
		}
		if(addTo != null)
			addTo.add(temp);
		return ret;
	}
}

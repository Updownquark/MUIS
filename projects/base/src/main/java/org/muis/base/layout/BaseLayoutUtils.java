package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.*;
import org.muis.core.style.Size;

/** Utilities for use by layouts in MUIS base */
public class BaseLayoutUtils {
	/**
	 * Gets the length of a layout of a set of widgets along a common axis
	 *
	 * @param children The children to lay out
	 * @param orient The orientation of the layout
	 * @param type The type of size to get
	 * @param crossSize The size of the container opposite to the layout's axis
	 * @param csMax Whether the cross size is to be considered a maximum
	 * @param marginX The margin size along the horizontal axis
	 * @param marginY The margin size along the vertical axis
	 * @param paddingX The padding size along the horizontal axis
	 * @param paddingY The padding size along the vertical axis
	 * @return The main size of the layout
	 */
	public static int getBoxLayoutSize(MuisElement [] children, Orientation orient, LayoutGuideType type, int crossSize, boolean csMax,
		Size marginX, Size marginY, Size paddingX, Size paddingY) {
		LayoutSize temp = new LayoutSize();
		for(int i = 0; i < children.length; i++) {
			if(i == 0)
				temp.add(orient == Orientation.horizontal ? marginX : marginY);
			else
				temp.add(orient == Orientation.horizontal ? paddingX : paddingY);
			if(i == children.length - 1)
				temp.add(orient == Orientation.horizontal ? marginX : marginY);
			LayoutUtils.getSize(children[i], orient, type, Integer.MAX_VALUE, crossSize, csMax, temp);
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
	public static int getBoxLayoutCrossSize(MuisElement [] children, Orientation orient, LayoutGuideType type, int mainSize,
		boolean sizeMax, LayoutSize addTo) {
		LayoutSize temp = new LayoutSize(true);
		int ret = 0;
		for(MuisElement child : children) {
			int sz = LayoutUtils.getSize(child, orient.opposite(), type, Integer.MAX_VALUE, mainSize, sizeMax, temp);
			if(sz > ret)
				ret = sz;
		}
		if(addTo != null)
			addTo.add(temp);
		return ret;
	}
}

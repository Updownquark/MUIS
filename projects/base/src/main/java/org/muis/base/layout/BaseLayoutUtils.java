package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.*;
import org.muis.core.style.Size;

public class BaseLayoutUtils {
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

	public static int getBoxLayoutCrossSize(MuisElement [] children, Orientation orient, LayoutGuideType type, int mainSize,
		boolean sizeMax, LayoutSize addTo) {
		LayoutSize temp = new LayoutSize(true);
		int ret = 0;
		for(MuisElement child : children) {
			int sz = LayoutUtils.getSize(child, orient, type, Integer.MAX_VALUE, mainSize, sizeMax, temp);
			if(sz > ret)
				ret = sz;
		}
		if(addTo != null)
			addTo.add(temp);
		return ret;
	}
}

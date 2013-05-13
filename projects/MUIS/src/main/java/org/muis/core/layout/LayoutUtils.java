package org.muis.core.layout;

import java.awt.Dimension;

import org.muis.core.MuisElement;
import org.muis.core.style.Size;

public class LayoutUtils {
	public static int getSize(MuisElement element, Orientation orientation, LayoutGuideType type, int parallelSize, int crossSize,
		boolean csMax, LayoutSize addTo) {
		LayoutAttributes.SizeAttribute att;
		Size ret;
		att = LayoutAttributes.getSizeAtt(orientation, null);
		ret = element.atts().get(att);
		if(ret == null && type != null) {
			att = LayoutAttributes.getSizeAtt(orientation, type);
			ret = element.atts().get(att);
		}
		if(ret != null) {
			if(addTo != null) {
				switch (ret.getUnit()) {
				case pixels:
				case lexips:
					addTo.add((int) ret.getValue());
					break;
				case percent:
					addTo.addPercent(ret.getValue());
					break;
				}
			}
			return ret.evaluate(parallelSize);
		} else if(type == null)
			return -1;
		else {
			int size = element.bounds().get(orientation).getGuide().get(type, crossSize, csMax);
			if(addTo != null)
				addTo.add(size);
			return size;
		}
	}

	public static int get(Dimension dim, Orientation orient) {
		switch (orient) {
		case horizontal:
			return dim.width;
		case vertical:
			return dim.height;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}

	public static void set(Dimension dim, Orientation orient, int size) {
		switch (orient) {
		case horizontal:
			dim.width = size;
			break;
		case vertical:
			dim.height = size;
			break;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}
}

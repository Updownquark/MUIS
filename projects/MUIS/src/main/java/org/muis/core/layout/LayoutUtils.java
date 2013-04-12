package org.muis.core.layout;

import org.muis.core.MuisElement;
import org.muis.core.style.Size;

public class LayoutUtils {
	public static int getSize(MuisElement element, Orientation orientation, LayoutGuideType type, int parallelSize, int crossSize,
		boolean csMax, LayoutSize addTo) {
		LayoutAttributes.SizeAttribute att = LayoutAttributes.getSizeAtt(orientation, type);
		Size ret = element.atts().get(att);
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
}

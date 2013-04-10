package org.muis.base.layout;

import org.muis.core.MuisElement;
import org.muis.core.layout.LayoutAttributes;
import org.muis.core.layout.LayoutGuideType;
import org.muis.core.layout.Orientation;
import org.muis.core.style.Size;

public class BaseLayoutUtils {
	public static int getBoxLayoutSize(MuisElement [] children, Orientation orient, LayoutGuideType type, int crossSize, boolean csMax) {
		int pixels = 0;
		float percent = 0;
		for(MuisElement child : children) {
			Size size = child.atts().get(LayoutAttributes.getSizeAtt(orient, type));
			if(size != null) {
				switch (size.getUnit()) {
				case pixels:
				case lexips: // Invalid, but don't bother throwing exception
					pixels += size.evaluate(0);
					break;
				case percent:
					percent += size.getValue();
					break;
				}
			} else {
				pixels += child.bounds().get(orient).getGuide().get(type, crossSize, csMax);
			}
		}
		if(pixels == 0) {
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
		if(percent == 0)
			return pixels;
		if(percent > 100)
			percent = 95;
		return Math.round(pixels / (1 - percent / 100));
	}

	public static int getFlowLayoutSize(MuisElement [] children, Orientation flow, Orientation orient, LayoutGuideType type, int crossSize,
		boolean csMax) {
		boolean main = flow == orient;
		switch (type) {
		case min:
		case minPref:
			if(main) {
				// Minimum means wrapping every single component
				int [] widths = new int[children.length];
				boolean [] widthPercents = new boolean[children.length];
				int [] heights = new int[1];
				boolean [] heightPercents = new boolean[children.length];
				int maxHeight = 0;
				for(int i = 0; i < children.length; i++) {
					MuisElement child = children[i];
					Size size = child.atts().get(LayoutAttributes.getSizeAtt(orient, type));
					if(size != null) {
						switch (size.getUnit()) {
						case pixels:
						case lexips: // Invalid, but don't bother throwing exception
							widths[i] = size.evaluate(0);
							if(widths[i] > maxWidth)
								maxWidth = widths[i];
							break;
						case percent:
							percent += size.getValue();
							break;
						}
					} else {
						pixels += child.bounds().get(orient).getGuide().get(type, crossSize, csMax);
					}
				}
			} else {
			}
			break;
		case pref:
		case maxPref:
		case max:
		}
	}
}

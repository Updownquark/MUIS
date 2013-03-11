package org.muis.core.layout;

import org.muis.core.MuisElement;
import org.muis.core.style.Size;

public class LayoutUtils {
	public static int getSize(MuisElement element, Orientation orientation, LayoutGuideType type, int parallelSize, int crossSize) {
		LayoutAttributes.SizeAttribute att=LayoutAttributes.getSizeAtt(orientation, type);
		Size ret=element.atts().get(att);
		if(ret!=null)
			return ret.evaluate(parallelSize);
		else if(type==null)
			return -1;
		else
			return element.bounds().get(orientation).getGuide().get(type, crossSize);
	}
}

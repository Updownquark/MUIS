package org.muis.core.layout;

public abstract class AbstractSizeGuide implements SizeGuide {
	@Override
	public int get(LayoutGuideType type, int crossSize) {
		switch (type) {
		case min:
			return getMin(crossSize);
		case minPref:
			return getMinPreferred(crossSize);
		case pref:
			return getPreferred(crossSize);
		case maxPref:
			return getMaxPreferred(crossSize);
		case max:
			return getMax(crossSize);
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + type);
	}
}

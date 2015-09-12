package org.quick.core.layout;

/** Implements the {@link SizeGuide#get(LayoutGuideType, int, boolean)} method */
public abstract class AbstractSizeGuide implements SizeGuide {
	@Override
	public int get(LayoutGuideType type, int crossSize, boolean csMax) {
		switch (type) {
		case min:
			return getMin(crossSize, csMax);
		case minPref:
			return getMinPreferred(crossSize, csMax);
		case pref:
			return getPreferred(crossSize, csMax);
		case maxPref:
			return getMaxPreferred(crossSize, csMax);
		case max:
			return getMax(crossSize, csMax);
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + type);
	}
}

package org.muis.core.layout;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.muis.core.MuisElement;
import org.muis.core.style.Size;

public class LayoutUtils {
	public static class LayoutInterpolation<T> {
		public final LayoutGuideType lowerType;

		public final double proportion;

		public final T lowerValue;

		public final T upperValue;

		public LayoutInterpolation(LayoutGuideType type, double prop, T lowValue, T highValue) {
			lowerType = type;
			proportion = prop;
			lowerValue = lowValue;
			upperValue = highValue;
		}
	}

	public static interface LayoutChecker<T> {
		T getLayoutValue(LayoutGuideType type);

		int getSize(T layoutValue);
	}

	public static int getSize(MuisElement element, Orientation orientation, LayoutGuideType type, int parallelSize, int crossSize,
		boolean csMax, LayoutSize addTo) {
		LayoutAttributes.SizeAttribute att;
		Size ret;
		att = LayoutAttributes.getSizeAtt(orientation, null);
		ret = element.atts().get(att);
		if(ret == null && type != null) {
			att = LayoutAttributes.getSizeAtt(orientation, type);
			if(att != null)
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
	}

	public static int getPos(Rectangle rect, Orientation orient) {
		switch (orient) {
		case horizontal:
			return rect.x;
		case vertical:
			return rect.y;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}

	public static void setPos(Rectangle rect, Orientation orient, int size) {
		switch (orient) {
		case horizontal:
			rect.x = size;
			break;
		case vertical:
			rect.y = size;
			break;
		}
	}

	public static int getSize(Rectangle rect, Orientation orient) {
		switch (orient) {
		case horizontal:
			return rect.width;
		case vertical:
			return rect.height;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}

	public static void setSize(Rectangle rect, Orientation orient, int size) {
		switch (orient) {
		case horizontal:
			rect.width = size;
			break;
		case vertical:
			rect.height = size;
			break;
		}
	}

	public static <T> LayoutInterpolation<T> interpolate(LayoutChecker<T> checker, int size, boolean useMin, boolean useMax) {
		T prefValue = checker.getLayoutValue(LayoutGuideType.pref);
		int prefSize = checker.getSize(prefValue);
		if(prefSize > size) {
			if(useMin) {
				T minValue = checker.getLayoutValue(LayoutGuideType.min);
				int minSize = checker.getSize(minValue);
				if(minSize == prefSize)
					return new LayoutInterpolation<>(LayoutGuideType.pref, 0, prefValue, prefValue);
				else if(minSize >= size)
					return new LayoutInterpolation<>(LayoutGuideType.min, 0, minValue, minValue);
				else {
					T minPrefValue = checker.getLayoutValue(LayoutGuideType.minPref);
					int minPrefSize = checker.getSize(minPrefValue);
					if(minPrefSize > size) {
						// Some proportion between min and minPref
						double prop = (size - minSize) * 1.0 / (minPrefSize - minSize);
						return new LayoutInterpolation<>(LayoutGuideType.min, prop, minValue, minPrefValue);
					} else if(minPrefSize < size) {
						// Some proportion between minPref and pref
						double prop = (size - minPrefSize) * 1.0 / (prefSize - minPrefSize);
						return new LayoutInterpolation<>(LayoutGuideType.minPref, prop, minPrefValue, prefValue);
					} else
						return new LayoutInterpolation<>(LayoutGuideType.minPref, 0, minPrefValue, minPrefValue);
				}
			} else {
				T minPrefValue = checker.getLayoutValue(LayoutGuideType.minPref);
				int minPrefSize = checker.getSize(minPrefValue);
				if(minPrefSize > size)
					return new LayoutInterpolation<>(LayoutGuideType.minPref, 0, minPrefValue, minPrefValue);
				else if(minPrefSize < size) {
					// Some proportion between minPref and pref
					double prop = (size - minPrefSize) * 1.0 / (prefSize - minPrefSize);
					return new LayoutInterpolation<>(LayoutGuideType.minPref, prop, minPrefValue, prefValue);
				} else if(minPrefSize == prefSize)
					return new LayoutInterpolation<>(LayoutGuideType.pref, 0, prefValue, prefValue);
				else
					return new LayoutInterpolation<>(LayoutGuideType.minPref, 0, minPrefValue, minPrefValue);
			}
		} else if(prefSize < size) {
			if(useMax){
				T maxValue = checker.getLayoutValue(LayoutGuideType.max);
				int maxSize = checker.getSize(maxValue);
				if(maxSize>size){
					T maxPrefValue = checker.getLayoutValue(LayoutGuideType.maxPref);
					int maxPrefSize = checker.getSize(maxPrefValue);
					if(maxPrefSize>size){
						// Some proportion between pref and maxPref
						double prop = (size - prefSize) * 1.0 / (maxPrefSize - prefSize);
						return new LayoutInterpolation<>(LayoutGuideType.pref, prop, prefValue, maxPrefValue);
					} else if(maxPrefSize<size){
						// Some proportion between maxPref and max
						double prop = (size - maxPrefSize) * 1.0 / (maxSize - maxPrefSize);
						return new LayoutInterpolation<>(LayoutGuideType.maxPref, prop, maxPrefValue, maxValue);
					} else
						return new LayoutInterpolation<>(LayoutGuideType.maxPref, 0, maxPrefValue, maxPrefValue);
				} else
					return new LayoutInterpolation<>(LayoutGuideType.max, 0, maxValue, maxValue);
			} else {
				T maxPrefValue = checker.getLayoutValue(LayoutGuideType.maxPref);
				int maxPrefSize = checker.getSize(maxPrefValue);
				if(maxPrefSize > size) {
					// Some proportion between pref and maxPref
					double prop = (size - prefSize) * 1.0 / (maxPrefSize - prefSize);
					return new LayoutInterpolation<>(LayoutGuideType.pref, prop, prefValue, maxPrefValue);
				} else
					return new LayoutInterpolation<>(LayoutGuideType.maxPref, 0, maxPrefValue, maxPrefValue);
			}
		} else
			return new LayoutInterpolation<>(LayoutGuideType.pref, 0, prefValue, prefValue);
	}
}

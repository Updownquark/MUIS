package org.quick.core.layout;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.quick.core.QuickElement;
import org.quick.core.style.Size;

/** A set of utilities for layouts */
public class LayoutUtils {
	/**
	 * The result of an {@link LayoutUtils#interpolate(LayoutChecker, int, LayoutGuideType, LayoutGuideType) interpolate} operation
	 * 
	 * @param <T> The type of value used in the operation
	 */
	public static class LayoutInterpolation<T> {
		/** The guide type under or at the interpolation result */
		public final LayoutGuideType lowerType;

		/** A number between 0 and 1 detailing how far above the {@link #lowerType} the interpolation result was */
		public final double proportion;

		/** The value for the guide type under or at the interpolation result */
		public final T lowerValue;

		/** The value for the guide type over or at the interpolation result */
		public final T upperValue;

		/**
		 * @param type The guide type under or at the interpolation result
		 * @param prop A number between 0 and 1 detailing how far above the {@link #lowerType} the interpolation result was
		 * @param lowValue The value for the guide type under or at the interpolation result
		 * @param highValue The value for the guide type over or at the interpolation result
		 */
		public LayoutInterpolation(LayoutGuideType type, double prop, T lowValue, T highValue) {
			lowerType = type;
			proportion = prop;
			lowerValue = lowValue;
			upperValue = highValue;
		}
	}

	/**
	 * Calling-code feedback to {@link LayoutUtils#interpolate(LayoutChecker, int, LayoutGuideType, LayoutGuideType) interpolate}
	 * 
	 * @param <T> The type of the value to use in the interpolation
	 */
	public static interface LayoutChecker<T> {
		/**
		 * @param type The guide type to get the layout value for
		 * @return The layout value for the given guide type
		 */
		T getLayoutValue(LayoutGuideType type);

		/**
		 * @param layoutValue The value to get the size for
		 * @return The pixel size of the layout value
		 */
		int getSize(T layoutValue);
	}

	/**
	 * @param element The element to get the intended size of
	 * @param orientation The orientation along which to get the element's intended size
	 * @param type The type of size to get
	 * @param parallelSize The size of the element's container along the given orientation
	 * @param crossSize The size of the element's container along the axis opposite to the given orientation
	 * @param csMax Whether the cross size is intended as a maximum or a real container value
	 * @param addTo The layout size to add the result to (may be null)
	 * @return The intended pixel-size of the element
	 */
	public static int getSize(QuickElement element, Orientation orientation, LayoutGuideType type, int parallelSize, int crossSize,
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

	/**
	 * @param dim The dimension to get the size of
	 * @param orient The orientation to get the size along
	 * @return The dimensions's size along the given orientation
	 */
	public static int get(Dimension dim, Orientation orient) {
		switch (orient) {
		case horizontal:
			return dim.width;
		case vertical:
			return dim.height;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}

	/**
	 * @param dim The dimension to set the size of
	 * @param orient The orientation to set the size along
	 * @param size The size for the dimension along the given orientation
	 */
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

	/**
	 * @param rect The rectangle to get the position of
	 * @param orient The orientation to get the position along
	 * @return The rectangle's position along the given orientation
	 */
	public static int getPos(Rectangle rect, Orientation orient) {
		switch (orient) {
		case horizontal:
			return rect.x;
		case vertical:
			return rect.y;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}

	/**
	 * @param rect The rectangle to set the position of
	 * @param orient The orientation to set the position along
	 * @param pos The position for the rectangle along the given orientation
	 */
	public static void setPos(Rectangle rect, Orientation orient, int pos) {
		switch (orient) {
		case horizontal:
			rect.x = pos;
			break;
		case vertical:
			rect.y = pos;
			break;
		}
	}

	/**
	 * @param rect The rectangle to get the size of
	 * @param orient The orientation to get the size along
	 * @return The rectangle's size along the given orientation
	 */
	public static int getSize(Rectangle rect, Orientation orient) {
		switch (orient) {
		case horizontal:
			return rect.width;
		case vertical:
			return rect.height;
		}
		throw new IllegalStateException("Unrecognized orientation: " + orient);
	}

	/**
	 * @param rect The rectangle to set the size of
	 * @param orient The orientation to set the size along
	 * @param size The size for the rectangle along the given orientation
	 */
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

	/**
	 * Determines which layout guide type (or proportion in between) works best for a problem
	 *
	 * @param <T> The type of the layout value to use
	 * @param checker The calling-code feedback to the interpolator
	 * @param size The constraint size
	 * @param minType The minimum size type to use
	 * @param maxType The maximum size type to use
	 * @return The interpolation result
	 */
	public static <T> LayoutInterpolation<T> interpolate(LayoutChecker<T> checker, int size, LayoutGuideType minType,
		LayoutGuideType maxType) {
		if(minType == null)
			minType = LayoutGuideType.min;
		if(maxType == null)
			maxType = LayoutGuideType.max;
		if(minType.compareTo(LayoutGuideType.pref) > 0)
			throw new IllegalArgumentException("minType may be " + LayoutGuideType.pref + ", " + LayoutGuideType.minPref + ", or "
				+ LayoutGuideType.min);
		if(maxType.compareTo(LayoutGuideType.pref) < 0)
			throw new IllegalArgumentException("maxType may be " + LayoutGuideType.pref + ", " + LayoutGuideType.maxPref + ", or "
				+ LayoutGuideType.max);
		T prefValue = checker.getLayoutValue(LayoutGuideType.pref);
		int prefSize = checker.getSize(prefValue);
		if(prefSize > size) {
			switch (minType) {
			case min:
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
			case minPref:
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
			default:
				return new LayoutInterpolation<>(LayoutGuideType.pref, 0, prefValue, prefValue);
			}
		} else if(prefSize < size) {
			switch (maxType) {
			case max:
				T maxValue = checker.getLayoutValue(LayoutGuideType.max);
				int maxSize = checker.getSize(maxValue);
				if(maxSize > size) {
					T maxPrefValue = checker.getLayoutValue(LayoutGuideType.maxPref);
					int maxPrefSize = checker.getSize(maxPrefValue);
					if(maxPrefSize > size) {
						// Some proportion between pref and maxPref
						double prop = (size - prefSize) * 1.0 / (maxPrefSize - prefSize);
						return new LayoutInterpolation<>(LayoutGuideType.pref, prop, prefValue, maxPrefValue);
					} else if(maxPrefSize < size) {
						// Some proportion between maxPref and max
						double prop = (size - maxPrefSize) * 1.0 / (maxSize - maxPrefSize);
						return new LayoutInterpolation<>(LayoutGuideType.maxPref, prop, maxPrefValue, maxValue);
					} else
						return new LayoutInterpolation<>(LayoutGuideType.maxPref, 0, maxPrefValue, maxPrefValue);
				} else
					return new LayoutInterpolation<>(LayoutGuideType.max, 0, maxValue, maxValue);
			case maxPref:
				T maxPrefValue = checker.getLayoutValue(LayoutGuideType.maxPref);
				int maxPrefSize = checker.getSize(maxPrefValue);
				if(maxPrefSize > size) {
					// Some proportion between pref and maxPref
					double prop = (size - prefSize) * 1.0 / (maxPrefSize - prefSize);
					return new LayoutInterpolation<>(LayoutGuideType.pref, prop, prefValue, maxPrefValue);
				} else
					return new LayoutInterpolation<>(LayoutGuideType.maxPref, 0, maxPrefValue, maxPrefValue);
			default:
				return new LayoutInterpolation<>(LayoutGuideType.pref, 0, prefValue, prefValue);
			}
		} else
			return new LayoutInterpolation<>(LayoutGuideType.pref, 0, prefValue, prefValue);
	}
}

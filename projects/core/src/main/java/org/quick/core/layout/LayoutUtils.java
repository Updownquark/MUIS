package org.quick.core.layout;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.quick.core.QuickElement;
import org.quick.core.layout.LayoutAttributes.SizeAttribute;
import org.quick.core.style.LengthUnit;
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
	 * Checks a child in a traditional layout for compatibility of attributes, logging errors if any
	 *
	 * @param child The child to check
	 * @return Whether the child passed the check
	 */
	public static boolean checkLayoutChild(QuickElement child) {
		for (Orientation orient : Orientation.values()) {
			SizeAttribute sizeAttr = LayoutAttributes.getSizeAtt(orient, null);
			SizeAttribute minAttr = LayoutAttributes.getSizeAtt(orient, LayoutGuideType.min);
			SizeAttribute maxAttr = LayoutAttributes.getSizeAtt(orient, LayoutGuideType.max);
			Size size = child.atts().get(sizeAttr).get();
			Size minSize = child.atts().get(minAttr).get();
			Size maxSize = child.atts().get(maxAttr).get();
			if (size != null && (minSize != null || maxSize != null))
				child.msg().warn(minAttr + " and/or " + maxAttr + " attributes are set at the same time as " + sizeAttr);
			if (minSize != null && maxSize != null && minSize.getUnit() != maxSize.getUnit()) {
				child.msg().error(minAttr + " and " + maxAttr + " must have the same unit");
				return false;
			}
		}
		return true;
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
		Size minSize = element.atts().getValue(LayoutAttributes.getSizeAtt(orientation, LayoutGuideType.min), null);
		Size maxSize = element.atts().getValue(LayoutAttributes.getSizeAtt(orientation, LayoutGuideType.max), null);
		ret = element.atts().getValue(att, null);
		if (ret == null && type != null && !type.isPref())
			ret = type.isMin() ? minSize : maxSize;
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
			return constrain(ret.evaluate(parallelSize), minSize, maxSize, parallelSize);
		} else if(type == null)
			return -1;
		else {
			int size = element.bounds().get(orientation).getGuide()//
				.get(type, crossSize, csMax);
			if(addTo != null)
				addTo.add(size);
			return size;
		}
	}

	private static int constrain(int size, Size minSize, Size maxSize, int parallelSize) {
		int minSz = minSize == null ? -1 : minSize.evaluate(parallelSize);
		if (minSize != null && size < minSz)
			return minSz;
		int maxSz = maxSize == null ? -1 : maxSize.evaluate(parallelSize);
		if (maxSize != null && size > maxSz) {
			if (minSz >= 0 && maxSz < minSz)
				return minSz;
			else
				return maxSz;
		}
		return size;
	}

	/**
	 * @param element The element to get the size for
	 * @param orientation The orientation along which to get the size
	 * @param type The type of the size
	 * @param crossSize The cross size
	 * @param csMax Whether the cross size is a maximum or an absolute value
	 * @return Either a 1- or a 3-length array of sizes. If 1-length, it is the size for the element. If 3-length, it is the min, pref, and
	 *         max sizes for the element, where min and max are determined by attributes set on the element. The 3-length array is only
	 *         returned when the units for the min and max attributes are incompatible with the unit for the preferred value, which may be
	 *         determined by an attribute or the child's own layout manager.
	 */
	public static Size[] getLayoutSize(QuickElement element, Orientation orientation, LayoutGuideType type, int crossSize, boolean csMax) {
		Size ret = element.atts().get(LayoutAttributes.getSizeAtt(orientation, null)).get();
		if (ret != null)
			return new Size[] { ret };
		Size minSize = element.atts().get(LayoutAttributes.getSizeAtt(orientation, LayoutGuideType.min)).get();
		Size maxSize = element.atts().get(LayoutAttributes.getSizeAtt(orientation, LayoutGuideType.max)).get();
		if (minSize != null && maxSize != null && minSize.compareTo(maxSize) >= 0)
			return new Size[] { minSize };
		Size layoutSize = new Size(element.bounds().get(orientation).getGuide()//
			.get(type, crossSize, csMax), LengthUnit.pixels);
		if (minSize != null && minSize.getUnit() == layoutSize.getUnit()) {
			if (layoutSize.compareTo(minSize) < 0)
				layoutSize = minSize;
			minSize = null;
		}
		if (maxSize != null && maxSize.getUnit() == layoutSize.getUnit()) {
			if (layoutSize.compareTo(maxSize) > 0)
				layoutSize = maxSize;
			maxSize = null;
		}
		if (minSize == null && maxSize == null)
			return new Size[] { layoutSize };
		else
			return new Size[] { minSize, layoutSize, maxSize };
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

	/**
	 * Adds 2 sizes, accounting for overflow (capping at {@link Integer#MAX_VALUE})
	 *
	 * @param size1 The first size to add
	 * @param size2 The second size to add
	 * @return The sum of the two sizes, capped at {@link Integer#MAX_VALUE}
	 */
	public static int add(int size1, int size2) {
		if (size1 == Integer.MAX_VALUE || size2 == Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		int res = size1 + size2;
		if (res < size1)
			return Integer.MAX_VALUE;
		return res;
	}

	/**
	 * Adds a corner radius to a size
	 *
	 * @param size The size
	 * @param radius The corner radius
	 * @return The new size
	 */
	public static int addRadius(int size, Size radius) {
		switch (radius.getUnit()) {
		case pixels:
		case lexips:
			size = add(size, Math.round(radius.getValue() * 2));
			break;
		case percent:
			float radPercent = radius.getValue() * 2;
			if (radPercent >= 100)
				radPercent = 90;
			size = Math.round(size * 100f / (100f - radPercent));
			break;
		}
		return size;
	}

	/**
	 * Subtracts a corner radius from a size
	 *
	 * @param size The size
	 * @param radius The corner radius
	 * @return The new size
	 */
	public static int removeRadius(int size, Size radius) {
		return Math.max(0, size - radius.evaluate(size) * 2);
	}
}

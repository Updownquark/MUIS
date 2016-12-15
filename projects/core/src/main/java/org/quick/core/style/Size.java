package org.quick.core.style;

import java.util.Objects;

/** Represents a 1-dimensional size along an axis */
public class Size implements Comparable<Size> {
	private final float theValue;

	private final LengthUnit theUnit;

	/** Creates a Size (0px) */
	public Size() {
		theValue = 0;
		theUnit = LengthUnit.pixels;
	}

	/**
	 * Creates a size with the given value and unit
	 *
	 * @param value The value for the size
	 * @param unit The unit for the size
	 */
	public Size(float value, LengthUnit unit) {
		theValue = value;
		theUnit = unit;
	}

	/** @return This size's value */
	public float getValue() {
		return theValue;
	}

	/** @return This length's unit */
	public LengthUnit getUnit() {
		return theUnit;
	}

	/**
	 * Evaluates a size for a particular case
	 *
	 * @param totalSize The size of the element to use if this size is relative.
	 * @return The number of pixels that this size represents in the given case
	 */
	public int evaluate(int totalSize) {
		switch (theUnit) {
		case pixels:
			return Math.round(theValue);
		case percent:
			return Math.round(theValue * totalSize / 100);
		case lexips:
			throw new IllegalStateException("Unrecognized size unit: " + theUnit);
		}
		throw new IllegalStateException("Unrecognized size unit: " + theUnit);
	}

	@Override
	public int compareTo(Size o) {
		if(theUnit == o.theUnit)
			return Float.compare(theValue, o.theValue);
		switch (theUnit) {
		case pixels:
			switch (o.theUnit) {
			case pixels:
				return Float.compare(theValue, o.theValue);
			case percent:
				if(theValue == 0)
					return Float.compare(theValue, o.theValue);
				else
					throw new IllegalArgumentException("Cannot compare non-zero sizes of units " + theUnit + " and " + o.theUnit);
			case lexips:
				throw new IllegalArgumentException("Cannot compare sizes of units " + theUnit + " and " + o.theUnit);
			}
			break;
		case percent:
			switch (o.theUnit) {
			case pixels:
				if(theValue == 0)
					return Float.compare(theValue, o.theValue);
				else
					throw new IllegalArgumentException("Cannot compare non-zero sizes of units " + theUnit + " and " + o.theUnit);
			case percent:
				return Float.compare(theValue, o.theValue);
			case lexips:
				throw new IllegalArgumentException("Cannot compare sizes of units " + theUnit + " and " + o.theUnit);
			}
			break;
		case lexips:
			throw new IllegalArgumentException("Cannot compare sizes of units " + theUnit + " and " + o.theUnit);
		}
		throw new IllegalStateException("Unrecognized sizes units: " + theUnit + " or " + o.theUnit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(theValue, theUnit);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Size))
			return false;
		Size sz = (Size) obj;
		return theValue == sz.theValue && theUnit == sz.theUnit;
	}

	@Override
	public String toString() {
		return theValue + " " + theUnit;
	}

	/**
	 * @param pixels The number of pixels to create the size for
	 * @return The new size
	 */
	public static Size pix(float pixels) {
		return new Size(pixels, LengthUnit.pixels);
	}

	/**
	 * @param percent The percent size to create the size for
	 * @return The new size
	 */
	public static Size pc(float percent) {
		return new Size(percent, LengthUnit.percent);
	}

	/**
	 * @param lexips The number of pixels less than the maximum to create the size for
	 * @return The new size
	 */
	public static Size lx(float lexips) {
		return new Size(lexips, LengthUnit.lexips);
	}
}

package org.muis.core.style;

/** Represents a 1-dimensional position along an axis */
public class Position implements Comparable<Position> {
	private float theValue;

	private LengthUnit theUnit;

	/** Creates a Position (0px) */
	public Position() {
		theValue = 0;
		theUnit = LengthUnit.pixels;
	}

	/**
	 * Creates a position with the given value and unit
	 *
	 * @param value The value for the position
	 * @param unit The unit for the position
	 */
	public Position(float value, LengthUnit unit) {
		theValue = value;
		theUnit = unit;
	}

	/** @return This position's value */
	public float getValue() {
		return theValue;
	}

	/** @return This position's unit */
	public LengthUnit getUnit() {
		return theUnit;
	}

	/** @param value The value for this position */
	public void setValue(float value) {
		theValue = value;
	}

	/** @param unit The unit for this position */
	public void setUnit(LengthUnit unit) {
		theUnit = unit;
	}

	/**
	 * Evaluates a position for a particular case
	 *
	 * @param totalSize The size of the element to use if this position is relative.
	 * @return The number of pixels that this position represents in the given case
	 */
	public int evaluate(int totalSize) {
		switch (theUnit) {
		case pixels:
			return Math.round(theValue);
		case percent:
			return Math.round(theValue * totalSize / 100);
		case lexips:
			return totalSize - Math.round(theValue);
		}
		throw new IllegalStateException("Unrecognized position unit: " + theUnit);
	}

	@Override
	public int compareTo(Position o) {
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
					throw new IllegalArgumentException("Cannot compare non-zero positions of units " + theUnit + " and " + o.theUnit);
			case lexips:
				throw new IllegalArgumentException("Cannot compare positions of units " + theUnit + " and " + o.theUnit);
			}
			break;
		case percent:
			switch (o.theUnit) {
			case pixels:
				if(theValue == 0)
					return Float.compare(theValue, o.theValue);
				else
					throw new IllegalArgumentException("Cannot compare non-zero positions of units " + theUnit + " and " + o.theUnit);
			case percent:
				return Float.compare(theValue, o.theValue);
			case lexips:
				throw new IllegalArgumentException("Cannot compare positions of units " + theUnit + " and " + o.theUnit);
			}
			break;
		case lexips:
			throw new IllegalArgumentException("Cannot compare positions of units " + theUnit + " and " + o.theUnit);
		}
		throw new IllegalStateException("Unrecognized position units: " + theUnit + " or " + o.theUnit);
	}

	@Override
	public String toString() {
		return theValue + " " + theUnit;
	}

	/**
	 * @param pixels The number of pixels to create the position for
	 * @return The new position
	 */
	public static Position pix(float pixels) {
		return new Position(pixels, LengthUnit.pixels);
	}

	/**
	 * @param percent The percent size to create the position for
	 * @return The new position
	 */
	public static Position pc(float percent) {
		return new Position(percent, LengthUnit.percent);
	}

	/**
	 * @param lexips The number of pixels less than the maximum to create the position for
	 * @return The new position
	 */
	public static Position lx(float lexips) {
		return new Position(lexips, LengthUnit.lexips);
	}
}

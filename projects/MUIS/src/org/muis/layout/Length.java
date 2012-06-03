package org.muis.layout;

/** Represents a setting for the length of a dimension of a MuisElement */
public class Length
{
	private float theValue;

	private LengthUnit theUnit;

	/** Creates a Length (0px) */
	public Length()
	{
		theValue = 0;
		theUnit = LengthUnit.pixels;
	}

	/**
	 * Creates a length with the given value and unit
	 * 
	 * @param value The value for the length
	 * @param unit The unit for the length
	 */
	public Length(float value, LengthUnit unit)
	{
		theValue = value;
		theUnit = unit;
	}

	/** @return This length's value */
	public float getValue()
	{
		return theValue;
	}

	/** @return This length's unit */
	public LengthUnit getUnit()
	{
		return theUnit;
	}

	/** @param value The value for this length */
	public void setValue(float value)
	{
		theValue = value;
	}

	/** @param unit The unit for this length */
	public void setUnit(LengthUnit unit)
	{
		theUnit = unit;
	}

	/**
	 * Evaluates a length for a particular case
	 * 
	 * @param totalSize The size of the element to use if this length is a percentage.
	 * @return The number of pixels that this length represents in the given case
	 */
	public int evaluate(int totalSize)
	{
		switch(theUnit)
		{
		case pixels:
			return Math.round(theValue);
		case percent:
			return Math.round(theValue * totalSize);
		}
		throw new IllegalStateException("Unrecognized length unit: " + theUnit);
	}
}

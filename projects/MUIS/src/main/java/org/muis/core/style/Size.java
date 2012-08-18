package org.muis.core.style;

/** Represents a 1-dimensional size along an axis */
public class Size implements Comparable<Size>
{
	private float theValue;

	private LengthUnit theUnit;

	/** Creates a Size (0px) */
	public Size()
	{
		theValue = 0;
		theUnit = LengthUnit.pixels;
	}

	/** Creates a size with the given value and unit
	 *
	 * @param value The value for the size
	 * @param unit The unit for the size */
	public Size(float value, LengthUnit unit)
	{
		theValue = value;
		if(!unit.isSize())
			throw new IllegalArgumentException(unit + " is not a size unit");
		theUnit = unit;
	}

	/** @return This size's value */
	public float getValue()
	{
		return theValue;
	}

	/** @return This length's unit */
	public LengthUnit getUnit()
	{
		return theUnit;
	}

	/** @param value The value for this size */
	public void setValue(float value)
	{
		theValue = value;
	}

	/** @param unit The unit for this size */
	public void setUnit(LengthUnit unit)
	{
		theUnit = unit;
	}

	/** Evaluates a size for a particular case
	 *
	 * @param totalSize The size of the element to use if this size is relative.
	 * @return The number of pixels that this size represents in the given case */
	public int evaluate(int totalSize)
	{
		switch (theUnit)
		{
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
	public int compareTo(Size o)
	{
		if(theUnit == o.theUnit)
			return Float.compare(theValue, o.theValue);
		switch (theUnit)
		{
		case pixels:
			switch (o.theUnit)
			{
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
			switch (o.theUnit)
			{
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
	public String toString()
	{
		return theValue + " " + theUnit;
	}
}

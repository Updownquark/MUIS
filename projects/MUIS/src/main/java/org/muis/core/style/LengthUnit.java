package org.muis.core.style;

/** An enumeration of the different length unit options available in MUIS */
public enum LengthUnit
{
	// TODO ??? inches, centimeters, ems, etc.

	/** Represents a length in pixels */
	pixels("px", false, true),
	/** Represents a length as a fraction of the total length available in the parent */
	percent("%", true, true),
	/** Represents a length in pixels away from the far edge of the container */
	lexips("xp", true, false);

	/** The attribute value that represents this length unit */
	public final String attrValue;

	private boolean isRelative;

	private boolean isSize;

	LengthUnit(String attrVal, boolean relative, boolean size)
	{
		attrValue = attrVal;
		isRelative = relative;
		isSize = size;
	}

	/** @return Whether lengths of this unit are relative to another length */
	public boolean isRelative()
	{
		return isRelative;
	}

	/** @return Whether this length unit can be used in sizes or is just a positional unit */
	public boolean isSize()
	{
		return isSize;
	}

	@Override
	public String toString()
	{
		return attrValue;
	}
}

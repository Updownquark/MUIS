package org.muis.base.layout;

/** An enumeration of the different length unit options available in MUIS */
public enum LengthUnit
{
	// TODO ??? inches, centimeters, ems, etc.

	/** Represents a length in pixels */
	pixels("px", false),
	/** Represents a length as a fraction of the total length available in the parent */
	percent("%", true),
	/** Represents a length in pixels away from the far edget of the container */
	lexips("xp", true);

	/** The attribute value that represents this length unit */
	public final String attrValue;

	private boolean isRelative;

	LengthUnit(String attrVal, boolean relative)
	{
		attrValue = attrVal;
		isRelative = relative;
	}

	/** @return Whether lengths of this unit are relative to another length */
	public boolean isRelative()
	{
		return isRelative;
	}

	@Override
	public String toString()
	{
		return attrValue;
	}
}

package org.wam.layout;

/**
 * An enumeration of the different length unit options available in WAM
 */
public enum LengthUnit
{
	// TODO ??? inches, centimeters, ems, etc.

	/** Represents a length in pixels */
	pixels("px"),
	/** Represents a length as a fraction of the total length available in the parent */
	percent("%");

	/** The attribute value that represents this length unit */
	public final String attrValue;

	LengthUnit(String attrVal)
	{
		attrValue = attrVal;
	}

	@Override
	public String toString()
	{
		return attrValue;
	}
}

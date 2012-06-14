package org.muis.base.layout;

/** All possible directions for laying out items */
public enum Direction
{
	/** Items will be arranged top-to-bottom */
	DOWN(true, true),
	/** Items will be arranged bottom-to-top */
	UP(true, false),
	/** Items will be arranged left-to-right */
	RIGHT(false, true),
	/** Items will be arranged right-to-left */
	LEFT(false, false);

	private final boolean isVertical;

	private final boolean isPositive;

	Direction(boolean vert, boolean pos)
	{
		isVertical = vert;
		isPositive = pos;
	}

	/** @return Whether this direction is along the vertical or horizontal axis */
	public boolean isVertical()
	{
		return isVertical;
	}

	/** @return Whether this direction is positive with respect to either horizontal or vertical */
	public boolean isPositive()
	{
		return isPositive;
	}

	/**
	 * Retrieves a direction by name
	 *
	 * @param name The name of the direction to retrieve
	 * @param def The default value if the name does not match a direction--may be null
	 * @return The named direction, or <code>def</code> if <code>name</code> did not match a valid direction
	 */
	public static Direction byName(String name, Direction def)
	{
		for(Direction dir : values())
			if(dir.toString().equalsIgnoreCase(name))
				return dir;
		return def;
	}
}

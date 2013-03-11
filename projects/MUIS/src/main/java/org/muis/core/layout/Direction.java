package org.muis.core.layout;

import static org.muis.core.layout.Orientation.horizontal;
import static org.muis.core.layout.Orientation.vertical;

/** All possible directions for laying out items */
public enum Direction
{
	/** Items will be arranged top-to-bottom */
	DOWN(vertical, true),
	/** Items will be arranged bottom-to-top */
	UP(vertical, false),
	/** Items will be arranged left-to-right */
	RIGHT(horizontal, true),
	/** Items will be arranged right-to-left */
	LEFT(horizontal, false);

	private final Orientation theOrientation;

	private final boolean isPositive;

	Direction(Orientation orientation, boolean pos)
	{
		theOrientation = orientation;
		isPositive = pos;
	}

	/** @return This direction's orientation */
	public Orientation getOrientation()
	{
		return theOrientation;
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

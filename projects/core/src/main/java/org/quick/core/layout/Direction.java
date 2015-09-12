package org.quick.core.layout;

import static org.quick.core.layout.End.leading;
import static org.quick.core.layout.End.trailing;
import static org.quick.core.layout.Orientation.horizontal;
import static org.quick.core.layout.Orientation.vertical;

/** All possible directions for laying out items */
public enum Direction
{
	/** Items will be arranged top-to-bottom */
	DOWN(vertical, leading),
	/** Items will be arranged bottom-to-top */
	UP(vertical, trailing),
	/** Items will be arranged left-to-right */
	RIGHT(horizontal, leading),
	/** Items will be arranged right-to-left */
	LEFT(horizontal, trailing);

	private final Orientation theOrientation;

	private final End theStartEnd;

	Direction(Orientation orientation, End startEnd)
	{
		theOrientation = orientation;
		theStartEnd = startEnd;
	}

	/** @return This direction's orientation */
	public Orientation getOrientation()
	{
		return theOrientation;
	}

	/** @return Whether this direction is positive with respect to either horizontal or vertical */
	public End getStartEnd()
	{
		return theStartEnd;
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

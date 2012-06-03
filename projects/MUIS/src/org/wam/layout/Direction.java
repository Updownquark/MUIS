package org.wam.layout;

/** All possible directions for laying out items */
public enum Direction
{
	/** Items will be arranged bottom-to-top */
	UP,
	/** Items will be arranged top-to-bottom */
	DOWN,
	/** Items will be arranged right-to-left */
	LEFT,
	/** Items will be arranged left-to-right */
	RIGHT;

	/**
	 * Retrieves a direction by name
	 * 
	 * @param name The name of the direction to retrieve
	 * @param def The default value if the name does not match a direction--may be null
	 * @return The named direction, or <code>def</code> if <code>name</code> did not match a valid
	 *         direction
	 */
	public static Direction byName(String name, Direction def)
	{
		for(Direction dir : values())
			if(dir.toString().equalsIgnoreCase(name))
				return dir;
		return def;
	}
}

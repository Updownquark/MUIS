package org.muis.core;

/** A single mapping of a resource to a tag name */
public class ResourceMapping
{
	private final String theName;

	private final String theLocation;

	/**
	 * Creates a resource mapping
	 *
	 * @param name The tag name to use in a MUIS document
	 * @param location The location of the resource
	 */
	public ResourceMapping(String name, String location)
	{
		theName = name;
		theLocation = location;
	}

	/** @return The tag name to use in a MUIS document */
	public String getName()
	{
		return theName;
	}

	/** @return The location of the resource */
	public String getLocation()
	{
		return theLocation;
	}
}

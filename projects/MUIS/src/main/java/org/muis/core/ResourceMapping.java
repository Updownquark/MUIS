package org.muis.core;

/** A single mapping of a resource to a tag name */
public class ResourceMapping
{
	private final MuisToolkit theOwner;

	private final String theName;

	private final String theLocation;

	/**
	 * Creates a resource mapping
	 *
	 * @param owner The toolkit that owns this resource
	 * @param name The tag name to use in a MUIS document
	 * @param location The location of the resource
	 */
	public ResourceMapping(MuisToolkit owner, String name, String location)
	{
		theOwner = owner;
		theName = name;
		theLocation = location;
	}

	/** @return The toolkit that owns this resource */
	public MuisToolkit getOwner()
	{
		return theOwner;
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

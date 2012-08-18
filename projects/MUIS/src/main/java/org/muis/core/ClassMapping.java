package org.muis.core;

/**
 * A single mapping of a fully-qualified java class name to a tag name
 */
public class ClassMapping
{
	private final String theName;

	private final String theClassName;

	/**
	 * Creates a class mapping
	 * 
	 * @param name The tag name to use in a MUIS document
	 * @param className The fully-qualified java class name to map to the tag name
	 */
	public ClassMapping(String name, String className)
	{
		theName = name;
		theClassName = className;
	}

	/**
	 * @return The tag name to use in a MUIS document
	 */
	public String getName()
	{
		return theName;
	}

	/**
	 * @return The fully-qualified java class name mapped to the tag name
	 */
	public String getClassName()
	{
		return theClassName;
	}
}

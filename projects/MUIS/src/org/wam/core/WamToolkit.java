package org.wam.core;

import prisms.util.ArrayUtils;

/**
 * Represents a toolkit that contains resources for use in a WAM document
 */
public class WamToolkit extends java.net.URLClassLoader
{
	private boolean isSealed;

	private final java.net.URL theURI;

	private final String theName;

	private final String theDescription;

	private String theVersion;

	private ClassMapping [] theClassMappings;

	private WamToolkit [] theDependencies;

	private WamPermission [] thePermissions;

	/**
	 * Creates a WAM toolkit
	 * 
	 * @param defaultToolkit The default toolkit that loads core WAM types
	 * @param uri The URI location of the toolkit
	 * @param name The name of the toolkit
	 * @param descrip The description of the toolkit
	 * @param version The version of the toolkit
	 */
	public WamToolkit(WamToolkit defaultToolkit, java.net.URL uri, String name, String descrip,
		String version)
	{
		super(new java.net.URL[] {uri});
		theURI = uri;
		theName = name;
		theDescription = descrip;
		theVersion = version;
		theClassMappings = new ClassMapping[0];
		theDependencies = new WamToolkit[0];
		thePermissions = new WamPermission[0];
	}

	/**
	 * Creates the default WAM toolkit
	 * 
	 * @param uri The URI location of the toolkit
	 * @param name The name of the toolkit
	 * @param descrip The description of the toolkit
	 * @param version The version of the toolkit
	 */
	public WamToolkit(java.net.URL uri, String name, String descrip, String version)
	{
		super(new java.net.URL[] {uri});
		theURI = uri;
		theName = name;
		theDescription = descrip;
		theVersion = version;
		theClassMappings = new ClassMapping[0];
		theDependencies = new WamToolkit[0];
		thePermissions = new WamPermission[0];
	}

	/**
	 * @return The URI location of this toolkit
	 */
	public java.net.URL getURI()
	{
		return theURI;
	}

	/**
	 * @return This toolkit's name
	 */
	public String getName()
	{
		return theName;
	}

	/**
	 * @return This toolkit's description
	 */
	public String getDescription()
	{
		return theDescription;
	}

	/**
	 * @return This toolkit's version
	 */
	public String getVersion()
	{
		return theVersion;
	}

	/**
	 * Adds a toolkit dependency for this toolkit
	 * 
	 * @param toolkit The toolkit that this toolkit requires to perform its functionality
	 * @throws WamException If this toolkit has been sealed
	 */
	public void addDependency(WamToolkit toolkit) throws WamException
	{
		if(isSealed)
			throw new WamException("Cannot modify a sealed toolkit");
		theDependencies = ArrayUtils.add(theDependencies, toolkit);
	}

	/**
	 * Maps a class to a tag name for this toolkit
	 * 
	 * @param tagName The tag name to map the class to
	 * @param className The fully-qualified java class name to map the tag to
	 * @throws WamException If this toolkit has been sealed
	 */
	public void map(String tagName, String className) throws WamException
	{
		if(isSealed)
			throw new WamException("Cannot modify a sealed toolkit");
		theClassMappings = ArrayUtils.add(theClassMappings, new ClassMapping(tagName, className));
	}

	/**
	 * Adds a permission requirement to this toolkit
	 * 
	 * @param perm The permission that this toolkit requires or requests
	 * @throws WamException If this toolkit has been sealed
	 */
	public void addPermission(WamPermission perm) throws WamException
	{
		if(isSealed)
			throw new WamException("Cannot modify a sealed toolkit");
		thePermissions = ArrayUtils.add(thePermissions, perm);
	}

	/**
	 * @return Whether this toolkit has been sealed or not
	 */
	public final boolean isSealed()
	{
		return isSealed;
	}

	/**
	 * Seals this toolkit such that it cannot be modified
	 */
	public final void seal()
	{
		isSealed = true;
	}

	/**
	 * @return All tag names mapped to classes in this toolkit (not including dependencies)
	 */
	public String [] getTagNames()
	{
		String [] ret = new String[theClassMappings.length];
		for(int i = 0; i < theClassMappings.length; i++)
			ret[i] = theClassMappings[i].getName();
		return ret;
	}

	/**
	 * @param tagName The tag name mapped to the class to get
	 * @return The class name mapped to the tag name, or null if the tag name has not been mapped in
	 *         this toolkit
	 */
	public String getMappedClass(String tagName)
	{
		for(ClassMapping cm : theClassMappings)
			if(cm.getName().equals(tagName))
				return cm.getClassName();
		for(WamToolkit dependency : theDependencies)
		{
			String ret = dependency.getMappedClass(tagName);
			if(ret != null)
				return ret;
		}
		return null;
	}

	/**
	 * Loads a class from its fully-qualified java name and returns it as a implementation of an
	 * interface or a suclass of a super class
	 * 
	 * @param <T> The type of interface or superclass to return the class as
	 * @param name The fully-qualified java name of the class, not the WAM-tag name (e.g.
	 *            "org.wam.core.BlockElement", not "block")
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class
	 * @throws WamException If the class cannot be found, cannot be loaded, or is not an
	 *             subclass/implementation of the given class or interface
	 */
	public <T> Class<? extends T> loadClass(String name, Class<T> superClass) throws WamException
	{
		Class<?> loaded;
		try
		{
			loaded = loadClass(name);
		} catch(Throwable e)
		{
			throw new WamException("Could not load class " + name, e);
		}
		if(superClass == null) // This is acceptable--assume the null superClass arg is cast to
			return (Class<? extends T>) loaded; // Class<?>
		try
		{
			return loaded.asSubclass(superClass);
		} catch(ClassCastException e)
		{
			throw new WamException("Class " + loaded.getName() + " is not an instance of "
				+ superClass.getName());
		}
	}

	/**
	 * @return All toolkits that this toolkit depends on
	 */
	public WamToolkit [] getDependencies()
	{
		return theDependencies.clone();
	}

	/**
	 * @return All permissions that this toolkit requires or requests
	 */
	public WamPermission [] getPermissions()
	{
		return thePermissions.clone();
	}
}

package org.muis.core;

import java.net.URL;

import prisms.util.ArrayUtils;

/** Represents a toolkit that contains resources for use in a MUIS document */
public class MuisToolkit extends java.net.URLClassLoader
{
	private boolean isSealed;

	private final URL theURI;

	private final String theName;

	private final String theDescription;

	private String theVersion;

	private ClassMapping [] theClassMappings;

	private MuisToolkit [] theDependencies;

	private MuisPermission [] thePermissions;

	/**
	 * Creates a MUIS toolkit
	 *
	 * @param uri The URI location of the toolkit
	 * @param name The name of the toolkit
	 * @param descrip The description of the toolkit
	 * @param version The version of the toolkit
	 */
	public MuisToolkit(URL uri, String name, String descrip, String version)
	{
		super(new URL[0]);
		theURI = uri;
		theName = name;
		theDescription = descrip;
		theVersion = version;
		theClassMappings = new ClassMapping[0];
		theDependencies = new MuisToolkit[0];
		thePermissions = new MuisPermission[0];
	}

	/** @return The URI location of this toolkit */
	public URL getURI()
	{
		return theURI;
	}

	/** @return This toolkit's name */
	public String getName()
	{
		return theName;
	}

	/** @return This toolkit's description */
	public String getDescription()
	{
		return theDescription;
	}

	/** @return This toolkit's version */
	public String getVersion()
	{
		return theVersion;
	}

	/**
	 * @param classPath The class path to add to this toolkit
	 * @throws IllegalStateException If this toolkit has been sealed
	 */
	@Override
	public void addURL(URL classPath) throws IllegalStateException
	{
		if(isSealed)
			throw new IllegalStateException("Cannot modify a sealed toolkit");
		super.addURL(classPath);
	}

	/**
	 * Adds a toolkit dependency for this toolkit
	 *
	 * @param toolkit The toolkit that this toolkit requires to perform its functionality
	 * @throws MuisException If this toolkit has been sealed
	 */
	public void addDependency(MuisToolkit toolkit) throws MuisException
	{
		if(isSealed)
			throw new MuisException("Cannot modify a sealed toolkit");
		theDependencies = ArrayUtils.add(theDependencies, toolkit);
	}

	/**
	 * Maps a class to a tag name for this toolkit
	 *
	 * @param tagName The tag name to map the class to
	 * @param className The fully-qualified java class name to map the tag to
	 * @throws MuisException If this toolkit has been sealed
	 */
	public void map(String tagName, String className) throws MuisException
	{
		if(isSealed)
			throw new MuisException("Cannot modify a sealed toolkit");
		theClassMappings = ArrayUtils.add(theClassMappings, new ClassMapping(tagName, className));
	}

	/**
	 * Adds a permission requirement to this toolkit
	 *
	 * @param perm The permission that this toolkit requires or requests
	 * @throws MuisException If this toolkit has been sealed
	 */
	public void addPermission(MuisPermission perm) throws MuisException
	{
		if(isSealed)
			throw new MuisException("Cannot modify a sealed toolkit");
		thePermissions = ArrayUtils.add(thePermissions, perm);
	}

	/** @return Whether this toolkit has been sealed or not */
	public final boolean isSealed()
	{
		return isSealed;
	}

	/** Seals this toolkit such that it cannot be modified */
	public final void seal()
	{
		isSealed = true;
	}

	/** @return All tag names mapped to classes in this toolkit (not including dependencies) */
	public String [] getTagNames()
	{
		String [] ret = new String[theClassMappings.length];
		for(int i = 0; i < theClassMappings.length; i++)
			ret[i] = theClassMappings[i].getName();
		return ret;
	}

	/**
	 * @param tagName The tag name mapped to the class to get
	 * @return The class name mapped to the tag name, or null if the tag name has not been mapped in this toolkit
	 */
	public String getMappedClass(String tagName)
	{
		int sep = tagName.indexOf(':');
		if(sep >= 0)
			tagName = tagName.substring(sep + 1);
		for(ClassMapping cm : theClassMappings)
			if(cm.getName().equals(tagName))
				return cm.getClassName();
		for(MuisToolkit dependency : theDependencies)
		{
			String ret = dependency.getMappedClass(tagName);
			if(ret != null)
				return ret;
		}
		return null;
	}

	/**
	 * Loads a class from its fully-qualified java name and returns it as a implementation of an interface or a subclass of a super class
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param name The fully-qualified java name of the class, not the MUIS-tag name (e.g. "org.muis.core.BlockElement", not "block")
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class
	 * @throws MuisException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *             interface
	 */
	public <T> Class<? extends T> loadClass(String name, Class<T> superClass) throws MuisException
	{
		Class<?> loaded;
		try
		{
			loaded = loadClass(name);
		} catch(Throwable e)
		{
			throw new MuisException("Could not load class " + name, e);
		}
		if(superClass == null) // This is acceptable--assume the null superClass arg is cast to
			return (Class<? extends T>) loaded; // Class<?>
		try
		{
			return loaded.asSubclass(superClass);
		} catch(ClassCastException e)
		{
			throw new MuisException("Class " + loaded.getName() + " is not an instance of " + superClass.getName());
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
	{
		ClassNotFoundException cnfe;
		try
		{
			return super.loadClass(name, resolve);
		} catch(ClassNotFoundException e)
		{
			cnfe = e;
		}
		for(MuisToolkit depend : theDependencies)
			try
			{
				return depend.loadClass(name, resolve);
			} catch(ClassNotFoundException e)
			{
			}
		throw cnfe;
	}

	/**
	 * Attempts to find the given class, returning null the class resource cannot be found
	 *
	 * @param name The name of the class to try to find the definition for
	 * @return The class defined for the given name, if it could be found
	 */
	protected Class<?> tryFindClass(String name)
	{
		String path = name.replace('.', '/').concat(".class");
		for(URL url : getURLs())
		{
			String file = url.getFile();
			if(file.endsWith(".jar"))
				continue;
			if(!file.endsWith("/"))
				file += "/";
			file += path;
			URL res;
			try
			{
				res = new URL(url.getProtocol(), url.getHost(), file);
			} catch(java.net.MalformedURLException e)
			{
				System.err.println("Made a malformed URL during findClass!");
				e.printStackTrace();
				continue;
			}
			if(!checkURL(res))
				continue;
			java.io.ByteArrayOutputStream content = new java.io.ByteArrayOutputStream();
			java.io.InputStream input;
			try
			{
				input = res.openStream();
				int read = input.read();
				while(read >= 0)
				{
					content.write(read);
					read = input.read();
				}
			} catch(java.io.IOException e)
			{
				continue;
			}
			return defineClass(name, content.toByteArray(), 0, content.size());
		}
		return null;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		Class<?> ret = tryFindClass(name);
		if(ret != null)
			return ret;
		return super.findClass(name);
	}

	@Override
	public URL findResource(String name)
	{
		String path = name.replace('.', '/').concat(".class");
		for(URL url : getURLs())
		{
			String file = url.getFile();
			if(file.endsWith(".jar"))
				continue;
			if(!file.endsWith("/"))
				file += "/";
			file += path;
			URL res;
			try
			{
				res = new URL(url.getProtocol(), url.getHost(), file);
			} catch(java.net.MalformedURLException e)
			{
				System.err.println("Made a malformed URL during findClass!");
				e.printStackTrace();
				continue;
			}
			if(!checkURL(res))
				continue;
			return res;
		}
		return super.findResource(name);
	}

	private boolean checkURL(URL url)
	{
		java.net.URLConnection conn;
		try
		{
			conn = url.openConnection();
			conn.connect();
		} catch(java.io.IOException e)
		{
			return false;
		}
		return conn.getLastModified() > 0;
	}

	/** @return All toolkits that this toolkit depends on */
	public MuisToolkit [] getDependencies()
	{
		return theDependencies.clone();
	}

	/** @return All permissions that this toolkit requires or requests */
	public MuisPermission [] getPermissions()
	{
		return thePermissions.clone();
	}
}

package org.quick.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quick.core.parser.Version;

public abstract class QuickLibrary extends java.net.URLClassLoader {
	private final URL theURI;

	private final String theName;
	private final String theDescription;
	private final Version theVersion;

	private QuickLibrary thePriorityDependency;
	private final Map<String, String> theClassMappings;
	private final Map<String, String> theResourceMappings;
	private final List<? extends QuickLibrary> theDependencies;
	private final List<QuickPermission> thePermissions;

	protected QuickLibrary(URL uri, String name, String descrip, Version version, List<URL> cps, Map<String, String> classMap,
		Map<String, String> resMap, List<? extends QuickLibrary> depends, List<QuickPermission> perms) {
		super(new URL[0], null);
		theURI = uri;
		theName = name;
		theDescription = descrip;
		theVersion = version;
		theDependencies = Collections.unmodifiableList(new ArrayList<>(depends));
		thePermissions = Collections.unmodifiableList(new ArrayList<>(perms));
		theClassMappings = Collections.unmodifiableMap(new LinkedHashMap<>(classMap));
		theResourceMappings = Collections.unmodifiableMap(new LinkedHashMap<>(resMap));

		for (URL cp : cps)
			super.addURL(cp);
	}

	protected void setPriorityDependency(QuickLibrary dependency) {
		thePriorityDependency = dependency;
	}

	/** @return The URI location of this library */
	public URL getURI() {
		return theURI;
	}

	/** @return This library's name */
	public String getName() {
		return theName;
	}

	/** @return This library's description */
	public String getDescription() {
		return theDescription;
	}

	/** @return This library's version */
	public Version getVersion() {
		return theVersion;
	}

	/** @return All tag names mapped to classes in this library (not including dependencies) */
	public Set<String> getTagNames() {
		return Collections.unmodifiableSet(theClassMappings.keySet());
	}

	/**
	 * @param tagName The tag name mapped to the class to get
	 * @return The class name mapped to the tag name, or null if the tag name has not been mapped in this library
	 */
	public String getMappedClass(String tagName) {
		if (thePriorityDependency != null) {
			String priMapped = thePriorityDependency.getMappedClass(tagName);
			if (priMapped != null)
				return priMapped;
		}
		int sep = tagName.indexOf(':');
		if (sep >= 0)
			tagName = tagName.substring(sep + 1);
		String className = theClassMappings.get(tagName);
		if (className == null) {
			for (QuickLibrary dependency : theDependencies) {
				className = dependency.getMappedClass(tagName);
				if (className != null)
					return className;
			}
		}
		return className;
	}

	/**
	 * @param tagName The tag name mapped to the resource to get
	 * @return The location of the resource mapped to the tag name, or null if the tag name has not been mapped in this library
	 */
	public String getMappedResource(String tagName) {
		if (thePriorityDependency != null) {
			String priMapped = thePriorityDependency.getMappedResource(tagName);
			if (priMapped != null)
				return priMapped;
		}
		int sep = tagName.indexOf(':');
		if (sep >= 0)
			tagName = tagName.substring(sep + 1);
		String res = theResourceMappings.get(tagName);
		if (res == null) {
			for (QuickLibrary dependency : theDependencies) {
				res = dependency.getMappedResource(tagName);
				if (res != null)
					return res;
			}
		}
		return res;
	}

	/**
	 * WILL FAIL--class paths cannot be added to a QuickLibarary dynamically
	 *
	 * @param classPath The class path to add to this library
	 */
	@Override
	public void addURL(URL classPath) {
		throw new IllegalStateException(
			"addURL cannot be called directly on a QuickLibrary.  All classpaths must be set using the builder.");
	}

	/**
	 * Loads a class from its fully-qualified java name and returns it as a implementation of an interface or a subclass of a super class
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param name The fully-qualified java name of the class, not the Quick-tag name (e.g. "org.quick.core.BlockElement", not "block")
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class
	 * @throws QuickException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *         interface
	 */
	public <T> Class<? extends T> loadClass(String name, Class<T> superClass) throws QuickException {
		Class<?> loaded;
		try {
			loaded = loadClass(name);
		} catch (Throwable e) {
			throw new QuickException("Could not load class " + name, e);
		}
		if (superClass == null) // This is acceptable--assume the null superClass arg is cast to
			return (Class<? extends T>) loaded; // Class<?>
		try {
			return loaded.asSubclass(superClass);
		} catch (ClassCastException e) {
			throw new QuickException("Class " + loaded.getName() + " is not an instance of " + superClass.getName(), e);
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		ClassNotFoundException cnfe;
		if (thePriorityDependency != null)
			try {
				thePriorityDependency.loadClass(name, resolve);
			} catch (ClassNotFoundException e) {
			}
		try {
			return super.loadClass(name, resolve);
		} catch (ClassNotFoundException e) {
			cnfe = e;
		}
		for (QuickLibrary depend : theDependencies)
			try {
				return depend.loadClass(name, resolve);
			} catch (ClassNotFoundException e) {
			}
		try {
			return ClassLoader.getSystemClassLoader().loadClass(name);
		} catch (ClassNotFoundException __e) {
			throw new ClassNotFoundException(theName + " could not load class " + name, cnfe);
		}
	}

	/**
	 * Attempts to find the given class, returning null the class resource cannot be found
	 *
	 * @param name The name of the class to try to find the definition for
	 * @return The class defined for the given name, if it could be found
	 */
	protected Class<?> tryFindClass(String name) {
		String path = name.replace('.', '/').concat(".class");
		for (URL url : getURLs()) {
			String file = url.getFile();
			if (file.endsWith(".jar"))
				continue;
			if (!file.endsWith("/"))
				file += "/";
			file += path;
			URL res;
			try {
				res = new URL(url.getProtocol(), url.getHost(), file);
			} catch (java.net.MalformedURLException e) {
				System.err.println("Made a malformed URL during findClass!");
				e.printStackTrace();
				continue;
			}
			if (!checkURL(res))
				continue;
			java.io.ByteArrayOutputStream content = new java.io.ByteArrayOutputStream();
			java.io.InputStream input;
			try {
				input = res.openStream();
				int read = input.read();
				while (read >= 0) {
					content.write(read);
					read = input.read();
				}
			} catch (java.io.IOException e) {
				continue;
			}
			return defineClass(name, content.toByteArray(), 0, content.size());
		}
		return null;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> ret = tryFindClass(name);
		if (ret != null)
			return ret;
		return super.findClass(name);
	}

	@Override
	public URL findResource(String name) {
		for (URL url : getURLs()) {
			String file = url.getFile();
			if (file.endsWith(".jar"))
				continue;
			if (!file.endsWith("/"))
				file += "/";
			if (name.startsWith("/"))
				file += name.substring(1);
			else
				file += name;
			URL res;
			try {
				res = new URL(url.getProtocol(), url.getHost(), file);
			} catch (java.net.MalformedURLException e) {
				System.err.println("Made a malformed URL during findClass!");
				e.printStackTrace();
				continue;
			}
			if (!checkURL(res))
				continue;
			return res;
		}
		return super.findResource(name);
	}

	private boolean checkURL(URL url) {
		java.net.URLConnection conn;
		try {
			conn = url.openConnection();
			conn.connect();
		} catch (java.io.IOException e) {
			return false;
		}
		return conn.getLastModified() > 0;
	}

	protected QuickLibrary getPriorityDependency() {
		return thePriorityDependency;
	}

	/** @return All libraries that this library depends on */
	public List<? extends QuickLibrary> getDependencies() {
		return theDependencies;
	}

	/** @return All permissions that this library requires or requests */
	public List<QuickPermission> getPermissions() {
		return thePermissions;
	}

	@Override
	public String toString() {
		return theName + " v" + theVersion;
	}

	/** Builds a QuickLibrary */
	public static abstract class Builder<L extends QuickLibrary, B extends Builder<L, B>> {
		private final URL theLocation;

		private String theName;
		private String theDescription;
		private Version theVersion;

		private List<URL> theClassPaths;
		private List<QuickLibrary> theDependencies;
		private Map<String, String> theClassMappings;
		private Map<String, String> theResourceLocations;
		private List<QuickPermission> thePermissions;

		private L theBuiltLibrary;

		protected Builder(URL location) {
			theLocation = location;
			theClassPaths = new ArrayList<>();
			theDependencies = new ArrayList<>();
			theClassMappings = new LinkedHashMap<>();
			theResourceLocations = new LinkedHashMap<>();
			thePermissions = new ArrayList<>();
		}

		public abstract String getLibraryType();

		public URL getLocation() {
			return theLocation;
		}

		/**
		 * @param name The name for this library
		 * @return This builder, for chaining
		 */
		public B setName(String name) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theName = name;
			return (B) this;
		}

		/**
		 * @param descrip The description for this library
		 * @return This builder, for chaining
		 */
		public B setDescription(String descrip) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theDescription = descrip;
			return (B) this;
		}

		/**
		 * @param version The version for this library
		 * @return This builder, for chaining
		 */
		public B setVersion(Version version) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theVersion = version;
			return (B) this;
		}

		/**
		 * @param classPath The class path to add to this library
		 * @return This builder, for chaining
		 */
		public B addClassPath(URL classPath) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theClassPaths.add(classPath);
			return (B) this;
		}

		/**
		 * Adds a toolkit dependency for this library
		 *
		 * @param lib The library that this library requires to perform its functionality
		 * @return This builder, for chaining
		 */
		public B addDependency(QuickLibrary lib) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theDependencies.add(lib);
			return (B) this;
		}

		/**
		 * Maps a class to a tag name for this library
		 *
		 * @param tagName The tag name to map the class to
		 * @param className The fully-qualified java class name to map the tag to
		 * @return This builder, for chaining
		 */
		public B map(String tagName, String className) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theClassMappings.put(tagName, className);
			return (B) this;
		}

		/**
		 * Maps a resource to a tag name for this library
		 *
		 * @param tagName The tag name to map the resource to
		 * @param resourceLocation The location of the resource to map the tag to
		 * @return This builder, for chaining
		 */
		public B mapResource(String tagName, String resourceLocation) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the " + getLibraryType() + " is built");
			theResourceLocations.put(tagName, resourceLocation);
			return (B) this;
		}

		/**
		 * Adds a permission requirement to this library
		 *
		 * @param perm The permission that this library requires or requests
		 * @return This builder, for chaining
		 */
		public B addPermission(QuickPermission perm) {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The builder may not be changed after the library is built");
			thePermissions.add(perm);
			return (B) this;
		}

		/** @return The built library */
		public L build() {
			if (theBuiltLibrary != null)
				throw new IllegalStateException("The " + getLibraryType() + " may only be built once");
			if (theName == null)
				throw new IllegalStateException("No name set");
			if (theDescription == null)
				throw new IllegalStateException("No description set");
			if (theVersion == null)
				throw new IllegalStateException("No version set");

			theBuiltLibrary = createLibrary(theLocation, theName, theDescription, theVersion, theClassPaths, theClassMappings,
				theResourceLocations, theDependencies, thePermissions);
			return theBuiltLibrary;
		}

		public abstract L createLibrary(URL location, String name, String description, Version version, List<URL> classPaths,
			Map<String, String> classMappings, Map<String, String> resourceLocations, List<QuickLibrary> dependencies,
			List<QuickPermission> permissions);

		public L getBuilt() {
			if (theBuiltLibrary == null)
				throw new IllegalStateException("Not built");
			return theBuiltLibrary;
		}
	}
}

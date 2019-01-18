package org.quick.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.observe.Observable;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.parser.Version;
import org.quick.core.style.CompoundStyleSheet;
import org.quick.core.style.MutableStyleSheet;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleCondition;
import org.quick.core.style.StyleConditionValue;
import org.quick.core.style.StyleSheet;

/** Represents a toolkit that contains resources for use in a Quick document */
public class QuickToolkit extends java.net.URLClassLoader {
	/** A toolkit style sheet contains no values itself, but serves as a container to hold all style sheets referred to by the toolkit */
	public class ToolkitStyleSheet extends CompoundStyleSheet {
		ToolkitStyleSheet(ObservableCollection<StyleSheet> dependencies) {
			super(dependencies);
		}

		@Override
		public String toString() {
			return "Style sheet for toolkit " + QuickToolkit.this;
		}
	}

	private final QuickEnvironment theEnvironment;
	private final URL theURI;

	private final String theName;
	private final String theDescription;
	private final Version theVersion;

	private final Map<String, String> theClassMappings;
	private final Map<String, String> theResourceMappings;
	private final List<QuickToolkit> theDependencies;
	private final List<QuickPermission> thePermissions;
	private final Observable<?> theDeath;

	private ToolkitStyleSheet theStyle;
	private ObservableCollection<StyleSheet> theStyleDependencyController;

	private QuickToolkit(QuickEnvironment env, URL uri, String name, String descrip, Version version, List<URL> cps,
		Map<String, String> classMap, Map<String, String> resMap, List<QuickToolkit> depends, List<QuickPermission> perms) {
		super(new URL[0], null);
		theEnvironment = env;
		theURI = uri;
		theName = name;
		theDescription = descrip;
		theVersion = version;
		theDependencies = Collections.unmodifiableList(new ArrayList<>(depends));
		thePermissions = Collections.unmodifiableList(new ArrayList<>(perms));
		theClassMappings = Collections.unmodifiableMap(new LinkedHashMap<>(classMap));
		theResourceMappings = Collections.unmodifiableMap(new LinkedHashMap<>(resMap));
		theStyleDependencyController = ObservableCollection.create(TypeTokens.get().of(StyleSheet.class));
		theStyle = new ToolkitStyleSheet(theStyleDependencyController.flow().unmodifiable().collect());
		// TODO Assuming toolkits are immortal right now.
		// If we ever want to remove them from memory when they're unused, replace this with something that can be called by the environment
		theDeath = Observable.empty();

		for(URL cp : cps)
			super.addURL(cp);
	}

	/** @return The environment that this toolkit is for */
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	/** @return The URI location of this toolkit */
	public URL getURI() {
		return theURI;
	}

	/** @return This toolkit's name */
	public String getName() {
		return theName;
	}

	/** @return This toolkit's description */
	public String getDescription() {
		return theDescription;
	}

	/** @return This toolkit's version */
	public Version getVersion() {
		return theVersion;
	}

	/** @return The styles for this toolkit */
	public ToolkitStyleSheet getStyle() {
		return theStyle;
	}

	/** @return All tag names mapped to classes in this toolkit (not including dependencies) */
	public Set<String> getTagNames() {
		return Collections.unmodifiableSet(theClassMappings.keySet());
	}

	/**
	 * @param tagName The tag name mapped to the class to get
	 * @return The class name mapped to the tag name, or null if the tag name has not been mapped in this toolkit
	 */
	public String getMappedClass(String tagName) {
		int sep = tagName.indexOf(':');
		if(sep >= 0)
			tagName = tagName.substring(sep + 1);
		String className = theClassMappings.get(tagName);
		if(className == null) {
			for(QuickToolkit dependency : theDependencies) {
				className = dependency.getMappedClass(tagName);
				if(className != null)
					return className;
			}
		}
		return className;
	}

	/**
	 * @param tagName The tag name mapped to the resource to get
	 * @return The location of the resource mapped to the tag name, or null if the tag name has not been mapped in this toolkit
	 */
	public String getMappedResource(String tagName) {
		int sep = tagName.indexOf(':');
		if(sep >= 0)
			tagName = tagName.substring(sep + 1);
		String res = theResourceMappings.get(tagName);
		if(res == null) {
			for(QuickToolkit dependency : theDependencies) {
				res = dependency.getMappedResource(tagName);
				if(res != null)
					return res;
			}
		}
		return res;
	}

	/** @param classPath The class path to add to this toolkit */
	@Override
	public void addURL(URL classPath) {
		throw new IllegalStateException(
			"addURL cannot be called directly on a QuickToolkit.  All classpaths must be set using the builder.");
	}

	/**
	 * Loads a class from its fully-qualified java name and returns it as a implementation of an interface or a subclass of a super class
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param name The fully-qualified java name of the class, not the Quick-tag name (e.g. "org.quick.core.BlockElement", not "block")
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class
	 * @throws QuickException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *             interface
	 */
	public <T> Class<? extends T> loadClass(String name, Class<T> superClass) throws QuickException {
		Class<?> loaded;
		try {
			loaded = loadClass(name);
		} catch(Throwable e) {
			throw new QuickException("Could not load class " + name, e);
		}
		if(superClass == null) // This is acceptable--assume the null superClass arg is cast to
			return (Class<? extends T>) loaded; // Class<?>
		try {
			return loaded.asSubclass(superClass);
		} catch(ClassCastException e) {
			throw new QuickException("Class " + loaded.getName() + " is not an instance of " + superClass.getName(), e);
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		ClassNotFoundException cnfe;
		try {
			return super.loadClass(name, resolve);
		} catch(ClassNotFoundException e) {
			cnfe = e;
		}
		for(QuickToolkit depend : theDependencies)
			try {
				return depend.loadClass(name, resolve);
			} catch(ClassNotFoundException e) {
			}
		try {
			return ClassLoader.getSystemClassLoader().loadClass(name);
		} catch (ClassNotFoundException __e) {
			throw new ClassNotFoundException("Toolkit " + theName + " could not load class " + name, cnfe);
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
		for(URL url : getURLs()) {
			String file = url.getFile();
			if(file.endsWith(".jar"))
				continue;
			if(!file.endsWith("/"))
				file += "/";
			file += path;
			URL res;
			try {
				res = new URL(url.getProtocol(), url.getHost(), file);
			} catch(java.net.MalformedURLException e) {
				System.err.println("Made a malformed URL during findClass!");
				e.printStackTrace();
				continue;
			}
			if(!checkURL(res))
				continue;
			java.io.ByteArrayOutputStream content = new java.io.ByteArrayOutputStream();
			java.io.InputStream input;
			try {
				input = res.openStream();
				int read = input.read();
				while(read >= 0) {
					content.write(read);
					read = input.read();
				}
			} catch(java.io.IOException e) {
				continue;
			}
			return defineClass(name, content.toByteArray(), 0, content.size());
		}
		return null;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> ret = tryFindClass(name);
		if(ret != null)
			return ret;
		return super.findClass(name);
	}

	@Override
	public URL findResource(String name) {
		for(URL url : getURLs()) {
			String file = url.getFile();
			if(file.endsWith(".jar"))
				continue;
			if(!file.endsWith("/"))
				file += "/";
			if(name.startsWith("/"))
				file += name.substring(1);
			else
				file += name;
			URL res;
			try {
				res = new URL(url.getProtocol(), url.getHost(), file);
			} catch(java.net.MalformedURLException e) {
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

	private boolean checkURL(URL url) {
		java.net.URLConnection conn;
		try {
			conn = url.openConnection();
			conn.connect();
		} catch(java.io.IOException e) {
			return false;
		}
		return conn.getLastModified() > 0;
	}

	/** @return All toolkits that this toolkit depends on */
	public List<QuickToolkit> getDependencies() {
		return theDependencies;
	}

	/** @return All permissions that this toolkit requires or requests */
	public List<QuickPermission> getPermissions() {
		return thePermissions;
	}

	@Override
	public String toString() {
		return theName + " v" + theVersion;
	}

	/**
	 * Creates a toolkit builder
	 *
	 * @param env The environment that the toolkit is in
	 * @param location The location of the toolkit
	 * @return The builder
	 */
	public static Builder build(QuickEnvironment env, URL location) {
		return new Builder(env, location);
	}

	/** Builds a QuickToolkit */
	public static class Builder {
		private final QuickEnvironment theEnvironment;
		private final URL theLocation;

		private String theName;
		private String theDescription;
		private Version theVersion;

		private List<URL> theClassPaths;
		private List<QuickToolkit> theDependencies;
		private Map<String, String> theClassMappings;
		private Map<String, String> theResourceLocations;
		private List<QuickPermission> thePermissions;

		private QuickToolkit theBuiltToolkit;

		private Builder(QuickEnvironment environment, URL location) {
			theEnvironment = environment;
			theLocation = location;
			theClassPaths = new ArrayList<>();
			theDependencies = new ArrayList<>();
			theClassMappings = new LinkedHashMap<>();
			theResourceLocations = new LinkedHashMap<>();
			thePermissions = new ArrayList<>();
		}

		/**
		 * @param name The name for this toolkit
		 * @return This builder, for chaining
		 */
		public Builder setName(String name) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theName = name;
			return this;
		}

		/**
		 * @param descrip The description for this toolkit
		 * @return This builder, for chaining
		 */
		public Builder setDescription(String descrip) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theDescription = descrip;
			return this;
		}

		/**
		 * @param version The version for this toolkit
		 * @return This builder, for chaining
		 */
		public Builder setVersion(Version version) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theVersion = version;
			return this;
		}

		/**
		 * @param classPath The class path to add to this toolkit
		 * @return This builder, for chaining
		 */
		public Builder addClassPath(URL classPath) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theClassPaths.add(classPath);
			return this;
		}

		/**
		 * Adds a toolkit dependency for this toolkit
		 *
		 * @param toolkit The toolkit that this toolkit requires to perform its functionality
		 * @return This builder, for chaining
		 */
		public Builder addDependency(QuickToolkit toolkit) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theDependencies.add(toolkit);
			return this;
		}

		/**
		 * Maps a class to a tag name for this toolkit
		 *
		 * @param tagName The tag name to map the class to
		 * @param className The fully-qualified java class name to map the tag to
		 * @return This builder, for chaining
		 */
		public Builder map(String tagName, String className) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theClassMappings.put(tagName, className);
			return this;
		}

		/**
		 * Maps a resource to a tag name for this toolkit
		 *
		 * @param tagName The tag name to map the resource to
		 * @param resourceLocation The location of the resource to map the tag to
		 * @return This builder, for chaining
		 */
		public Builder mapResource(String tagName, String resourceLocation) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			theResourceLocations.put(tagName, resourceLocation);
			return this;
		}

		/**
		 * Adds a permission requirement to this toolkit
		 *
		 * @param perm The permission that this toolkit requires or requests
		 * @return This builder, for chaining
		 */
		public Builder addPermission(QuickPermission perm) {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The builder may not be changed after the toolkit is built");
			thePermissions.add(perm);
			return this;
		}

		/** @return The built toolkit */
		public QuickToolkit build() {
			if(theBuiltToolkit != null)
				throw new IllegalStateException("The toolkit may only be built once");
			if(theName == null)
				throw new IllegalStateException("No name set");
			if(theDescription == null)
				throw new IllegalStateException("No description set");
			if(theVersion == null)
				throw new IllegalStateException("No version set");

			theBuiltToolkit = new QuickToolkit(theEnvironment, theLocation, theName, theDescription, theVersion, theClassPaths,
				theClassMappings, theResourceLocations, theDependencies, thePermissions);
			return theBuiltToolkit;
		}

		/**
		 * @param styleSheet The style sheet to use with this toolkit
		 * @return This builder, for chaining
		 */
		public Builder addStyleSheet(StyleSheet styleSheet) {
			if(theBuiltToolkit == null)
				throw new IllegalStateException("Styles must be added after the toolkit is built");
			if(!theLocation.equals(QuickEnvironment.CORE_TOOLKIT) && !(styleSheet instanceof ToolkitStyleSheet)) {
				for (StyleAttribute<?> attr : styleSheet.attributes()) {
					if(attr.getDomain().getClass().getClassLoader() == theBuiltToolkit)
						continue;
					for (StyleConditionValue<?> sev : styleSheet.getStyleExpressions(attr)) {
						if (sev.getCondition().getType().getClassLoader() == theBuiltToolkit)
							continue;
						if (checkRole(sev.getCondition().getRole(), sev.getCondition().getParent()))
							continue;
						String msg = "Toolkit " + theLocation + ": Style sheet";
						msg += " assigns styles for non-toolkit attributes to non-toolkit element types";
						if (!(styleSheet instanceof MutableStyleSheet))
							throw new IllegalStateException(msg);
						else {
							theEnvironment.msg().error(msg);
							((MutableStyleSheet) styleSheet).clear(attr, sev.getCondition());
						}
					}
				}
			}
			theBuiltToolkit.theStyleDependencyController.add(styleSheet);
			return this;
		}

		private boolean checkRole(AttachPoint<?> role, StyleCondition parent) {
			if(role==null)
				return true;
			if (role.template.getDefiner().getClassLoader() != theBuiltToolkit)
				return false;
			return checkRole(parent.getRole(), parent.getParent());
		}
	}
}

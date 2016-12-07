package org.quick.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Facilitates instantiating Quick classes via their namespace/tagname mappings */
public class QuickClassView {
	private final QuickEnvironment theEnvironment;

	private final QuickClassView theParent;

	private final QuickToolkit theMemberToolkit;

	private java.util.HashMap<String, QuickToolkit> theNamespaces;

	private boolean isSealed;

	/**
	 * @param env The environment to create the class view in
	 * @param parent The parent for this class view
	 * @param memberToolkit The toolkit that the owner of class view member belongs to
	 */
	public QuickClassView(QuickEnvironment env, QuickClassView parent, QuickToolkit memberToolkit) {
		if(env == null)
			throw new NullPointerException("environment is null");
		theEnvironment=env;
		theParent = parent;
		theMemberToolkit = memberToolkit;
		theNamespaces = new java.util.HashMap<>();
	}

	/** @return The environment that this class view is in */
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	/** @return This class view's parent */
	public QuickClassView getParent() {
		return theParent;
	}

	/** @return Whether this class view has been sealed or not */
	public final boolean isSealed() {
		return isSealed;
	}

	/** Seals this class view such that it cannot be modified */
	public final void seal() {
		isSealed = true;
	}

	/**
	 * Maps a namespace to a toolkit under this view
	 *
	 * @param namespace The namespace to map
	 * @param toolkit The toolkit to map the namespace to
	 * @throws QuickException If this class view is sealed
	 */
	public void addNamespace(String namespace, QuickToolkit toolkit) throws QuickException {
		if(isSealed)
			throw new QuickException("Cannot modify a sealed class view");
		theNamespaces.put(namespace, toolkit);
	}

	/**
	 * Gets the toolkit mapped to a given namespace
	 *
	 * @param namespace The namespace to get the toolkit for
	 * @return The toolkit mapped to the given namespace under this view, or null if the namespace is not mapped in this view
	 */
	public QuickToolkit getToolkit(String namespace) {
		QuickToolkit ret = theNamespaces.get(namespace);
		if(ret == null) {
			if(theParent != null)
				ret = theParent.getToolkit(namespace);
			else if(namespace == null)
				ret = theEnvironment.getCoreToolkit();
		}
		return ret;
	}

	/** @return The toolkits that may be used without specifying a namespace */
	public QuickToolkit [] getScopedToolkits() {
		java.util.LinkedHashSet<QuickToolkit> ret = new java.util.LinkedHashSet<>();
		if(theMemberToolkit != null)
			ret.add(theMemberToolkit);
		for(QuickToolkit tk : theNamespaces.values())
			ret.add(tk);
		if(theParent != null) {
			for(QuickToolkit tk : theParent.getScopedToolkits())
				ret.add(tk);
		} else
			ret.add(theEnvironment.getCoreToolkit());
		return ret.toArray(new QuickToolkit[ret.size()]);
	}

	/**
	 * Gets the toolkit that would load a class for a qualified name
	 *
	 * @param qName The qualified name to get the toolkit for
	 * @return The toolkit that can load the type with the given qualified name, or null if no such tag has been mapped in this view
	 */
	public QuickToolkit getToolkitForQName(String qName) {
		int idx = qName.indexOf(":");
		if(idx < 0) {
			for(QuickToolkit toolkit : getScopedToolkits()) {
				if(toolkit.getMappedClass(qName) != null)
					return toolkit;
			}
			return null;
		}
		else
			return getToolkit(qName.substring(0, idx));
	}

	/** @return All namespaces that have been mapped to toolkits in this class view */
	public Set<String> getMappedNamespaces() {
		LinkedHashSet<String> nss = new LinkedHashSet<>();
		addMappedNamespaces(nss);
		return Collections.unmodifiableSet(nss);
	}

	private void addMappedNamespaces(Set<String> nss) {
		nss.addAll(theNamespaces.keySet());
		if (theParent != null)
			theParent.addMappedNamespaces(nss);
	}

	/**
	 * Gets the fully-qualified class name mapped to a qualified tag name (namespace:tagName)
	 *
	 * @param qName The qualified tag name of the class to get
	 * @return The fully-qualified class name mapped to the qualified tag name, or null if no such class has been mapped in this view
	 */
	public String getMappedClass(String qName) {
		int idx = qName.indexOf(':');
		if(idx < 0)
			return getMappedClass(null, qName);
		else
			return getMappedClass(qName.substring(0, idx), qName.substring(idx + 1));
	}

	/**
	 * Gets the fully-qualified class name mapped to a tag name in this class view's domain
	 *
	 * @param namespace The namespace of the tag
	 * @param tag The tag name
	 * @return The fully-qualified class name mapped to the tag name, or null if no such class has been mapped in this domain
	 */
	public String getMappedClass(String namespace, String tag) {
		if(namespace == null) {
			for(QuickToolkit toolkit : getScopedToolkits()) {
				if(toolkit.getMappedClass(tag) != null)
					return toolkit.getMappedClass(tag);
			}
			return null;
		} else {
			QuickToolkit toolkit = getToolkit(namespace);
			if(toolkit == null)
				return null;
			return toolkit.getMappedClass(tag);
		}
	}

	/**
	 * @param qName The qualified name of the resource
	 * @return The mapped location of the resource, or null if no such resource is mapped
	 */
	public String getMappedResource(String qName) {
		int idx = qName.indexOf(':');
		if (idx < 0)
			return getMappedResource(null, qName);
		else
			return getMappedResource(qName.substring(0, idx), qName.substring(idx + 1));
	}

	/**
	 * @param namespace The namespace mapping the resource
	 * @param name The name of the mapped resource
	 * @return The mapped location of the resource, or null if no such resource is mapped
	 */
	public String getMappedResource(String namespace, String name) {
		if (namespace == null) {
			for (QuickToolkit toolkit : getScopedToolkits()) {
				String res = toolkit.getMappedResource(name);
				if (res != null)
					return res;
			}
			return null;
		} else {
			QuickToolkit toolkit = getToolkit(namespace);
			if (toolkit == null)
				return null;
			return toolkit.getMappedResource(name);
		}
	}

	/**
	 * Like {@link #loadIfMapped(String, Class)}, but throws an exception for unmapped tags
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param qName The qualified tag name
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class; or null if no such class has been mapped
	 *         in this domain
	 * @throws QuickException If the tag is not mapped or the class cannot be found, cannot be loaded, or is not an subclass/implementation
	 *         of the given class or interface
	 */
	public <T> Class<? extends T> loadMappedClass(String qName, Class<T> superClass) throws QuickException {
		int idx = qName.indexOf(':');
		String ns, tag;
		if(idx < 0) {
			ns = null;
			tag = qName;
		} else {
			ns = qName.substring(0, idx);
			tag = qName.substring(idx + 1);
		}
		return loadMappedClass(ns, tag, superClass);
	}

	/**
	 * Like {@link #loadIfMapped(String, String, Class)}, but throws an exception for unmapped tags
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param namespace The namespace of the tag
	 * @param tag The tag name
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class
	 * @throws QuickException If the tag is not mapped or the class cannot be found, cannot be loaded, or is not an subclass/implementation
	 *         of the given class or interface
	 */
	public <T> Class<? extends T> loadMappedClass(String namespace, String tag, Class<T> superClass) throws QuickException {
		Class<? extends T> loaded = loadIfMapped(namespace, tag, superClass);
		if (loaded == null) {
			if (namespace != null)
				throw new QuickException(
					"No class mapped to " + tag + " for namespace " + namespace + " (toolkit " + getToolkit(namespace).getName() + ")");
			else
				throw new QuickException("No class mapped to " + tag + " in scoped namespaces " + getMappedNamespaces());
		}
		return loaded;
	}

	/**
	 * A combination of {@link #getMappedClass(String)} and {@link QuickToolkit#loadClass(String, Class)} for simpler code.
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param qName The qualified tag name
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class; or null if no such class has been mapped
	 *         in this domain
	 * @throws QuickException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *         interface
	 */
	public <T> Class<? extends T> loadIfMapped(String qName, Class<T> superClass) throws QuickException {
		int idx = qName.indexOf(':');
		String ns, tag;
		if (idx < 0) {
			ns = null;
			tag = qName;
		} else {
			ns = qName.substring(0, idx);
			tag = qName.substring(idx + 1);
		}
		return loadIfMapped(ns, tag, superClass);
	}

	/**
	 * A combination of {@link #getMappedClass(String, String)} and {@link QuickToolkit#loadClass(String, Class)} for simpler code.
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param namespace The namespace of the tag
	 * @param tag The tag name
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class; or null if no such class has been mapped
	 *         in this domain
	 * @throws QuickException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *         interface
	 */
	public <T> Class<? extends T> loadIfMapped(String namespace, String tag, Class<T> superClass) throws QuickException {
		if(namespace != null) {
			QuickToolkit toolkit = getToolkit(namespace);
			if(toolkit == null)
				throw new QuickException("No toolkit mapped to namespace " + namespace);
			String className = toolkit.getMappedClass(tag);
			if (className != null)
				return toolkit.loadClass(className, superClass);
			throw new QuickException("No class mapped to " + namespace + ":" + tag + " in scoped namespaces " + getMappedNamespaces());
		} else {
			for(QuickToolkit toolkit : getScopedToolkits()) {
				String className = toolkit.getMappedClass(tag);
				if (className != null)
					return toolkit.loadClass(className, superClass);
			}
			throw new QuickException("No class mapped to " + tag + " in scoped namespaces " + getMappedNamespaces());
		}
	}

	/**
	 * Attempts to resolve the named class in all toolkits scoped in this class view
	 *
	 * @param s The fully-qualified class name of the class to resolve
	 * @return The resolved class
	 * @throws ClassNotFoundException If the class cannot be resolved in any toolkit scoped in this class view
	 */
	public Class<?> loadClass(String s) throws ClassNotFoundException {
		for (QuickToolkit toolkit : getScopedToolkits()) {
			try {
				return toolkit.loadClass(s);
			} catch (ClassNotFoundException e) {
			}
		}
		throw new ClassNotFoundException("Could not resolve class " + s + " in any scoped toolkit " + getMappedNamespaces());
	}
}

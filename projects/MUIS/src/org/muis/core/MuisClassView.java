package org.muis.core;

/** Facilitates instantiating MUIS classes via their namespace/tagname mappings */
public class MuisClassView
{
	private final MuisDocument theDocument;

	private final MuisElement theElement;

	private java.util.HashMap<String, MuisToolkit> theNamespaces;

	private boolean isSealed;

	/**
	 * @param doc The document to create the class view for
	 * @param el The element to create the class view for
	 */
	public MuisClassView(MuisDocument doc, MuisElement el)
	{
		if(doc == null)
			throw new NullPointerException("doc is null");
		if(el != null && el.getDocument() != null && el.getDocument() != doc)
			throw new IllegalArgumentException("Element is not in given document");
		theDocument = doc;
		theElement = el;
		theNamespaces = new java.util.HashMap<String, MuisToolkit>();
	}

	/**
	 * Creates a class map for an element
	 *
	 * @param el The element to create the class map for
	 */
	public MuisClassView(MuisElement el)
	{
		this(el.getDocument(), el);
	}

	/**
	 * Creates a class map for a document
	 *
	 * @param doc The document to create the class map for
	 */
	public MuisClassView(MuisDocument doc)
	{
		this(doc, null);
	}

	/** @return The document that owns the class map */
	public MuisDocument getDocument()
	{
		return theDocument;
	}

	/** @return This class map's element */
	public MuisElement getElement()
	{
		return theElement;
	}

	/** @return Whether this class view has been sealed or not */
	public final boolean isSealed()
	{
		return isSealed;
	}

	/** Seals this class view such that it cannot be modified */
	public final void seal()
	{
		isSealed = true;
	}

	/**
	 * Maps a namespace to a toolkit under this view
	 *
	 * @param namespace The namespace to map
	 * @param toolkit The toolkit to map the namespace to
	 * @throws MuisException If this class view is sealed
	 */
	public void addNamespace(String namespace, MuisToolkit toolkit) throws MuisException
	{
		if(isSealed)
			throw new MuisException("Cannot modify a sealed class view");
		theNamespaces.put(namespace, toolkit);
	}

	/**
	 * Gets the toolkit mapped to a given namespace
	 *
	 * @param namespace The namespace to get the toolkit for
	 * @return The toolkit mapped to the given namespace under this view, or null if the namespace is not mapped in this view
	 */
	public MuisToolkit getToolkit(String namespace)
	{
		MuisToolkit ret = theNamespaces.get(namespace);
		if(ret == null)
			if(theElement != null)
			{
				if(theElement.getParent() != null)
					ret = theElement.getParent().getClassView().getToolkit(namespace);
				else
					ret = theDocument.getClassView().getToolkit(namespace);
			}
			else if(namespace == null)
				if(theElement != null)
					ret = theElement.getDocument().getCoreToolkit();
				else
					ret = theDocument.getCoreToolkit();
		return ret;
	}

	/**
	 * Gets the toolkit that would load a class for a qualified name
	 *
	 * @param qName The qualified name to get the toolkit for
	 * @return The toolkit that can load the type with the given qualified name
	 */
	public MuisToolkit getToolkitForQName(String qName)
	{
		int idx = qName.indexOf(":");
		if(idx < 0)
			return getToolkit(null);
		else
			return getToolkit(qName.substring(0, idx));
	}

	/** @return All namespaces that have been mapped to toolkits in this class view */
	public String [] getMappedNamespaces()
	{
		return theNamespaces.keySet().toArray(new String[theNamespaces.size()]);
	}

	/**
	 * Gets the fully-qualified class name mapped to a qualified tag name (namespace:tagName)
	 *
	 * @param qName The qualified tag name of the class to get
	 * @return The fully-qualified class name mapped to the qualified tag name, or null if no such class has been mapped in this domain.
	 */
	public String getMappedClass(String qName)
	{
		int idx = qName.indexOf(':');
		if(idx < 0)
			return getMappedClass(null, qName);
		else
			return getMappedClass(qName.substring(0, idx), qName.substring(idx + 1));
	}

	/**
	 * Gets the fully-qualified class name mapped to a tag name in this class map's domain
	 *
	 * @param namespace The namespace of the tag
	 * @param tag The tag name
	 * @return The fully-qualified class name mapped to the tag name, or null if no such class has been mapped in this domain
	 */
	public String getMappedClass(String namespace, String tag)
	{
		MuisToolkit toolkit = getToolkit(namespace);
		if(toolkit == null)
			return null;
		return toolkit.getMappedClass(tag);
	}

	/**
	 * A combination of {@link #getMappedClass(String)} and {@link MuisToolkit#loadClass(String, Class)} for simpler code.
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param qName The qualified tag name
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class; or null if no such class has been mapped
	 *         in this domain
	 * @throws MuisException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *             interface
	 */
	public <T> Class<? extends T> loadMappedClass(String qName, Class<T> superClass) throws MuisException
	{
		int idx = qName.indexOf(':');
		String ns, tag;
		if(idx < 0)
		{
			ns = null;
			tag = qName;
		}
		else
		{
			ns = qName.substring(0, idx);
			tag = qName.substring(idx + 1);
		}
		return loadMappedClass(ns, tag, superClass);
	}

	/**
	 * A combination of {@link #getMappedClass(String, String)} and {@link MuisToolkit#loadClass(String, Class)} for simpler code.
	 *
	 * @param <T> The type of interface or superclass to return the class as
	 * @param namespace The namespace of the tag
	 * @param tag The tag name
	 * @param superClass The superclass or interface class to cast the class as an subclass of
	 * @return The loaded class, as an implementation or subclass of the interface or super class; or null if no such class has been mapped
	 *         in this domain
	 * @throws MuisException If the class cannot be found, cannot be loaded, or is not an subclass/implementation of the given class or
	 *             interface
	 */
	public <T> Class<? extends T> loadMappedClass(String namespace, String tag, Class<T> superClass) throws MuisException
	{
		MuisToolkit toolkit = getToolkit(namespace);
		if(toolkit == null)
			throw new MuisException("No toolkit mapped to namespace " + namespace);
		String className = toolkit.getMappedClass(tag);
		if(className == null)
			throw new MuisException("No class mapped to " + tag + " for namespace " + namespace + " (toolkit " + toolkit.getName() + ")");
		return toolkit.loadClass(className, superClass);
	}
}

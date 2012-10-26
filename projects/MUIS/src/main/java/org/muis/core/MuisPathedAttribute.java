package org.muis.core;

/**
 * Represents an attribute set with a path off of a root-level attribute
 *
 * @param <T> The type of the attribute
 */
public class MuisPathedAttribute<T> extends MuisAttribute<T> {
	private final MuisAttribute<T> theBase;

	private final String [] thePath;

	/**
	 * @param attr The base attribute that this attribute branches off of
	 * @param element The element to check the path's acceptance with
	 * @param path The path off of the base attribute
	 * @throws IllegalArgumentException If the given attribute does not accept the given path
	 */
	public MuisPathedAttribute(MuisAttribute<T> attr, MuisElement element, String... path) throws IllegalArgumentException {
		super(compile(attr.getName(), path), attr.getType(), attr.getValidator(), null);
		if(attr.getPathAccepter() == null)
			throw new IllegalArgumentException("Attribute " + attr.getName() + " is not hierarchical");
		if(!attr.getPathAccepter().accept(element, path))
			throw new IllegalArgumentException("Attribute " + attr.getName() + " does not accept path \"" + join(path) + "\"");
		theBase = attr;
		thePath = path;
	}

	/** @return The base attribute that this attribute branches off of */
	public MuisAttribute<T> getBase() {
		return theBase;
	}

	/** @return The path off of the base attribute */
	public String [] getPath() {
		return thePath;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof MuisPathedAttribute))
			return false;
		MuisPathedAttribute<?> attr = (MuisPathedAttribute<?>) o;
		return attr.theBase.equals(theBase) && prisms.util.ArrayUtils.equals(attr.thePath, thePath);
	}

	@Override
	public int hashCode() {
		return theBase.hashCode() * 7 + prisms.util.ArrayUtils.hashCode(thePath);
	}

	private static String compile(String base, String... path) {
		StringBuilder ret = new StringBuilder(base);
		for(String p : path)
			ret.append('.').append(p);
		return ret.toString();
	}

	private static String join(String... path) {
		if(path.length == 0)
			return "";
		StringBuilder ret = new StringBuilder(path[0]);
		for(int i = 1; i < path.length; i++)
			ret.append('.').append(path[i]);
		return ret.toString();
	}
}

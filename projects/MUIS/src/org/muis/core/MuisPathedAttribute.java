package org.muis.core;

public class MuisPathedAttribute<T> extends MuisAttribute<T> {
	private final MuisAttribute<T> theBase;

	private final String [] thePath;

	public MuisPathedAttribute(MuisAttribute<T> attr, String... path) {
		super(compile(attr.getName(), path), attr.getType(), attr.getValidator(), null);
		theBase = attr;
		thePath = path;
	}

	public MuisAttribute<T> getBase() {
		return theBase;
	}

	public String [] getPath() {
		return thePath;
	}

	private static String compile(String base, String... path) {
		StringBuilder ret = new StringBuilder(base);
		for(String p : path)
			ret.append('.').append(path);
		return ret.toString();
	}
}

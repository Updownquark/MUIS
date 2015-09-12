package org.quick.core.layout;

/** A 2-dimensional set of bounds */
public interface Bounds {
	/**
	 * @param orientation The orientation to get the bounds of
	 * @return The bounds in the given dimension
	 */
	public abstract BoundsDimension get(Orientation orientation);
}

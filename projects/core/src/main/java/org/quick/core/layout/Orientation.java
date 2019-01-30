package org.quick.core.layout;

/** Specifies orientation in 2D */
public enum Orientation {
	/** Horizontal (x-axis) orientation */
	horizontal,
	/** Vertical (y-axis) orientation */
	vertical;

	/**
	 * @param vert Whether to return {@link #vertical} or {@link #horizontal}
	 * @return The corresponding orientation
	 */
	public static Orientation of(boolean vert) {
		return vert ? vertical : horizontal;
	}

	/** @return The opposite orientation as this */
	public Orientation opposite() {
		switch (this) {
		case horizontal:
			return vertical;
		case vertical:
			return horizontal;
		}
		throw new IllegalStateException("Unrecognized orientation: " + this);
	}
}

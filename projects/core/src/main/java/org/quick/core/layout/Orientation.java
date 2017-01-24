package org.quick.core.layout;

/** Specifies orientation in 2D */
public enum Orientation {
	/** Horizontal (x-axis) orientation */
	horizontal,
	/** Vertical (y-axis) orientation */
	vertical;

	/** @return WHether this orientation is {@link #vertical}. Just a shortcut for <code>== Orientation.vertical</code> */
	public boolean isVertical() {
		return this == vertical;
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

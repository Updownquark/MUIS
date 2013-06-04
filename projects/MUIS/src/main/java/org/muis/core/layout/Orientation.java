package org.muis.core.layout;

/** Specifies orientation in 2D */
public enum Orientation {
	/** Horizontal (x-axis) orientation */
	horizontal,
	/** Vertical (y-axis) orientation */
	vertical;

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

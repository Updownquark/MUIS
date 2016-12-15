package org.quick.core.layout;

/** Represents regions in a container */
public enum Region {
	/** The top of the container */
	top,
	/** The left side of the container */
	left,
	/** The bottom of the container */
	bottom,
	/** The right side of the container */
	right,
	/** The center of the container */
	center;

	/** @return The orientation that this region represents */
	public Orientation getOrientation() {
		switch (this) {
		case top:
		case bottom:
			return Orientation.vertical;
		case left:
		case right:
			return Orientation.horizontal;
		case center:
			return null;
		}
		return null;
	}

	/** @return The end that this region represents */
	public End getEnd() {
		switch (this) {
		case top:
		case left:
			return End.leading;
		case bottom:
		case right:
			return End.trailing;
		case center:
			return null;
		}
		return null;
	}
}

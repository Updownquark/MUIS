package org.muis.core.layout;

public enum Orientation {
	horizontal, vertical;

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

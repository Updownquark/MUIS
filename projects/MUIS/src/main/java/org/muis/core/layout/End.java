package org.muis.core.layout;

public enum End {
	leading, trailing;

	public End opposite() {
		switch (this) {
		case leading:
			return trailing;
		case trailing:
			return leading;
		}
		throw new IllegalStateException("Unrecognized end: " + this);
	}
}

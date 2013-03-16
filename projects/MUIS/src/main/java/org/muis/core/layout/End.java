package org.muis.core.layout;

/** Ends of an axis */
public enum End {
	/** Left for {@link Orientation#horizontal horizontal}, top for {@link Orientation#vertical vertical} */
	leading,
	/** Right for {@link Orientation#horizontal horizontal}, bottom for {@link Orientation#vertical vertical} */
	trailing;

	/** @return The opposite end as this */
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

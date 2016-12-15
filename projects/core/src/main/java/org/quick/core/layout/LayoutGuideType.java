package org.quick.core.layout;

/** Represents a type of attribute in a size guide */
public enum LayoutGuideType {
	/** Represents a minimum size or position--the absolute minimum a value can be without violating the intentions of the guide */
	min(true, false, false),
	/** Represents a preferred minimum size or position--the minimum a value can be to satisfy the guide */
	minPref(true, true, false),
	/** Represents a preferred position or size--the value a guide would prefer to have if it is available */
	pref(false, true, false),
	/** Represents a preferred maximum size or position--the maximum a value can be to make full use of the guide */
	maxPref(false, true, true),
	/** Represents a maximum size or position--the absolute maximum a value can be without violating the intentions of the guide */
	max(false, false, true);

	private final boolean isMin;

	private final boolean isPref;

	private final boolean isMax;

	private LayoutGuideType(boolean _min, boolean _pref, boolean _max) {
		isMin = _min;
		isPref = _pref;
		isMax = _max;
	}

	/** @return Whether this is a minimum type ({@link #min} or {@link #minPref}) */
	public boolean isMin() {
		return isMin;
	}

	/** @return Whether this is a preferred type ({@link #minPref}, {@link #pref} or {@link #maxPref}) */
	public boolean isPref() {
		return isPref;
	}

	/** @return Whether this is a maximum type ({@link #max} or {@link #maxPref}) */
	public boolean isMax() {
		return isMax;
	}

	/** @return The next larger layout guide type */
	public LayoutGuideType next() {
		switch (this) {
		case min:
			return minPref;
		case minPref:
			return pref;
		case pref:
			return maxPref;
		case maxPref:
			return max;
		case max:
			return null;
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + this);
	}

	/**
	 * @return {@link #min} for {@link #min} or {@link #minPref}; {@link #pref} for {@link #pref}; {@link #max} for {@link #maxPref} or
	 *         {@link #max}
	 */
	public LayoutGuideType toExtreme() {
		switch (this) {
		case min:
		case pref:
		case max:
			return this;
		case minPref:
			return min;
		case maxPref:
			return max;
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + this);
	}

	/** @return The opposite of this layout type */
	public LayoutGuideType opposite() {
		switch (this) {
		case min:
			return max;
		case minPref:
			return maxPref;
		case pref:
			return pref;
		case maxPref:
			return minPref;
		case max:
			return min;
		}
		throw new IllegalStateException("Unrecognized layout guide type: " + this);
	}
}

package org.muis.core.layout;

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
}

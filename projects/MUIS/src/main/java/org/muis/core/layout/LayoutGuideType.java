package org.muis.core.layout;

/** Represents a type of attribute in a size guide */
public enum LayoutGuideType {
	/** Represents a minimum size or position--the absolute minimum a value can be without violating the intentions of the guide */
	min,
	/** Represents a preferred minimum size or position--the minimum a value can be to satisfy the guide */
	minPref,
	/** Represents a preferred position or size--the value a guide would prefer to have if it is available */
	pref,
	/** Represents a preferred maximum size or position--the maximum a value can be to make full use of the guide */
	maxPref,
	/** Represents a maximum size or position--the absolute maximum a value can be without violating the intentions of the guide */
	max;
}

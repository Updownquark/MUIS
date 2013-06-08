package org.muis.base.layout;

import org.muis.core.MuisAttribute;

/**
 * A flow layout is a layout that lays contents out one after another in one direction, perhaps using multiple rows, and obeys constraints
 * set on the contents.
 *
 * Deleted the guts of this. Everything's been changed so much since I attempted this. Only thing that was worth saving was the notion of
 * the break policy (not currently used anywhere).
 */
public abstract class AbstractFlowLayout {
	/** Policies for creating multiple rows (or columns) of content */
	public static enum BreakPolicy {
		/** Never breaks a line (row or column) of content */
		NEVER,
		/** Breaks content as needed to fit in a container (the default) */
		NEEDED,
		/** Attempts to make content fit squarely in its container, breaking as much as needed */
		SQUARE;
	}

	/** The attribute in the layout container that specifies the break policy for laying out items */
	public static final MuisAttribute<BreakPolicy> FLOW_BREAK = new MuisAttribute<>("flow-break",
		new org.muis.core.MuisProperty.MuisEnumProperty<>(BreakPolicy.class));
}

package org.muis.core.mgr;

/** Represents a set of active states */
public interface StateSet extends Iterable<MuisState> {
	/**
	 * @param state The state to check
	 * @return Whether the given state is currently active in this set
	 */
	boolean is(MuisState state);

	/** @return All states that are currently active in this set */
	MuisState [] toArray();
}

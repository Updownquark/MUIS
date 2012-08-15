package org.muis.core.mgr;

/** Represents a set of active states */
public interface StateSet extends Iterable<String> {
	/**
	 * @param state The state to check
	 * @return Whether the given state is currently active in this set
	 */
	boolean is(String state);

	/** @return All states that are currently active in this set */
	String [] toArray();
}

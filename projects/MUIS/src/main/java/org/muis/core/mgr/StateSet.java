package org.muis.core.mgr;

import org.muis.core.rx.ObservableSet;
import org.muis.core.rx.ObservableValue;

/** Represents a set of active states */
public interface StateSet extends Iterable<MuisState> {
	/**
	 * @param state The state to check
	 * @return Whether the given state is currently active in this set
	 */
	boolean is(MuisState state);

	/**
	 * @param state The state to observe
	 * @return An observable holding whether the state is active in this state set
	 */
	ObservableValue<Boolean> observe(MuisState state);

	/** @return All states that are currently active in this set */
	MuisState [] toArray();

	/** @return The observable set of all states controlled in this set */
	ObservableSet<MuisState> allStates();

	/** @return The observable set of all states that are active in this set */
	ObservableSet<MuisState> activeStates();
}

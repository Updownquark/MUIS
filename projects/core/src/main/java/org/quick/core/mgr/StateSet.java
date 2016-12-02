package org.quick.core.mgr;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;

/** Represents a set of active states */
public interface StateSet extends Iterable<QuickState> {
	/**
	 * @param state The state to check
	 * @return Whether the given state is currently active in this set
	 */
	boolean is(QuickState state);

	/**
	 * @param state The state to observe
	 * @return An observable holding whether the state is active in this state set
	 */
	ObservableValue<Boolean> observe(QuickState state);

	/** @return All states that are currently active in this set */
	QuickState [] toArray();

	/** @return The observable set of all states controlled in this set */
	ObservableSet<QuickState> allStates();

	/** @return The observable set of all states that are active in this set */
	ObservableSet<QuickState> activeStates();
}

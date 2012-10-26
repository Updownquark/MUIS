package org.muis.core.tags;

/** Tags a MuisElement as supporting a particular state (as an element in {@link StateSupport}) */
public @interface State {
	/** @return The name of the state */
	String name();

	/** @return The priority for the state */
	int priority();
}

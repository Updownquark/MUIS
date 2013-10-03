package org.muis.core.style.stateful;

import org.muis.core.mgr.MuisState;

/** Fired when the state of an {@link InternallyStatefulStyle} changes */
public class StateChangedEvent {
	private final InternallyStatefulStyle theStyle;

	private final MuisState [] theOldState;

	private final MuisState [] theNewState;

	/**
	 * @param style The style whose state has changed
	 * @param oldState The old state
	 * @param newState The new state
	 */
	public StateChangedEvent(InternallyStatefulStyle style, MuisState [] oldState, MuisState [] newState) {
		theStyle = style;
		theOldState = oldState;
		theNewState = newState;
	}

	/** @return The style whose state has changed */
	public InternallyStatefulStyle getStyle() {
		return theStyle;
	}

	/** @return The state prior to the change */
	public MuisState [] getOldState() {
		return theOldState;
	}

	/** @return The state after the change */
	public MuisState [] getNewState() {
		return theNewState;
	}
}

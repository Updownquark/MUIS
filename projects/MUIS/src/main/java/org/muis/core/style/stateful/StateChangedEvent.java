package org.muis.core.style.stateful;

import java.util.Set;

import org.muis.core.mgr.MuisState;

/** Fired when the state of an {@link InternallyStatefulStyle} changes */
public class StateChangedEvent extends org.muis.core.rx.ObservableValueEvent<Set<MuisState>> {
	private final InternallyStatefulStyle theStyle;

	/**
	 * @param style The style whose state has changed
	 * @param oldState The old state
	 * @param newState The new state
	 */
	public StateChangedEvent(InternallyStatefulStyle style, Set<MuisState> oldState, Set<MuisState> newState) {
		super(style.states(), oldState, newState, null);
		theStyle = style;
	}

	/** @return The style whose state has changed */
	public InternallyStatefulStyle getStyle() {
		return theStyle;
	}
}

package org.muis.core.style.stateful;

/** A listener to be notified when a {@link InternallyStatefulStyle}'s state changes */
public interface StateChangeListener {
	/** @param evt The event containing information about the state change */
	void stateChanged(StateChangedEvent evt);
}

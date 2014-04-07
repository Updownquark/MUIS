package org.muis.core.event;

import org.muis.core.MuisElement;
import org.muis.core.event.boole.TypedPredicate;
import org.muis.core.mgr.MuisState;

/** Represents a change to a {@link MuisState} in a {@link org.muis.core.mgr.StateEngine} */
public abstract class StateChangedEvent extends MuisPropertyEvent<Boolean> {
	/** Filters events of this type */
	@SuppressWarnings("hiding")
	public static final TypedPredicate<MuisEvent, StateChangedEvent> base = new TypedPredicate<MuisEvent, StateChangedEvent>() {
		@Override
		public StateChangedEvent cast(MuisEvent value) {
			return value instanceof StateChangedEvent ? (StateChangedEvent) value : null;
		}
	};

	/** A filter for state change events on a particular state */
	public static class StateTypedPredicate implements TypedPredicate<StateChangedEvent, StateChangedEvent> {
		private final MuisState theState;

		private StateTypedPredicate(MuisState att) {
			theState = att;
		}

		/** @return The state that this filter accepts events for */
		public MuisState getState() {
			return theState;
		}

		@Override
		public StateChangedEvent cast(StateChangedEvent value) {
			if(value.getState() == theState)
				return value;
			else
				return null;
		}
	}

	/**
	 * @param state The state to listen for
	 * @return A filter for change events to the given state
	 */
	public static TypedPredicate<MuisEvent, StateChangedEvent> state(MuisState state) {
		return new org.muis.core.event.boole.TPAnd<>(base, new StateTypedPredicate(state));
	}

	private final MuisState theState;

	private final MuisEvent theCause;

	/**
	 * @param element The element on which the state was changed
	 * @param state The state that was changed
	 * @param newValue Whether the state is now active or inactive
	 * @param cause The event that was the cause of the state change--may be null
	 */
	public StateChangedEvent(MuisElement element, MuisState state, boolean newValue, MuisEvent cause) {
		super(element, newValue);
		theState = state;
		theCause = cause;
	}

	/** @return The state that was changed */
	public MuisState getState() {
		return theState;
	}

	/** @return The event that was the cause of the state change--may be null */
	public MuisEvent getCause() {
		return theCause;
	}
}

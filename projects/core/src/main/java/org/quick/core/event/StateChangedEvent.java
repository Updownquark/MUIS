package org.quick.core.event;

import java.util.function.Function;

import org.quick.core.QuickElement;
import org.quick.core.event.boole.TypedPredicate;
import org.quick.core.mgr.QuickState;

/** Represents a change to a {@link QuickState} in a {@link org.quick.core.mgr.StateEngine} */
public abstract class StateChangedEvent extends QuickPropertyEvent<Boolean> {
	/** Filters events of this type */
	@SuppressWarnings("hiding")
	public static final Function<QuickEvent, StateChangedEvent> base = value -> {
		return value instanceof StateChangedEvent ? (StateChangedEvent) value : null;
	};

	/** A filter for state change events on a particular state */
	public static class StateTypedPredicate implements TypedPredicate<StateChangedEvent, StateChangedEvent> {
		private final QuickState theState;

		private StateTypedPredicate(QuickState att) {
			theState = att;
		}

		/** @return The state that this filter accepts events for */
		public QuickState getState() {
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
	public static Function<QuickEvent, StateChangedEvent> state(QuickState state) {
		return event -> {
			StateChangedEvent stateEvt = base.apply(event);
			if(stateEvt == null)
				return null;
			if(stateEvt.getState() != state)
				return null;
			return stateEvt;
		};
	}

	private final QuickState theState;

	private final QuickEvent theCause;

	/**
	 * @param element The element on which the state was changed
	 * @param state The state that was changed
	 * @param initial Whether this represents the population of the initial value of an observable value in response to subscription
	 * @param newValue Whether the state is now active or inactive
	 * @param cause The event that was the cause of the state change--may be null
	 */
	public StateChangedEvent(QuickElement element, QuickState state, boolean initial, boolean newValue, QuickEvent cause) {
		super(element, element.state().subscribe(state), initial, initial ? null : !newValue, newValue, cause);
		theState = state;
		theCause = cause;
	}

	/** @return The state that was changed */
	public QuickState getState() {
		return theState;
	}

	/** @return The event that was the cause of the state change--may be null */
	@Override
	public QuickEvent getCause() {
		return theCause;
	}

	@Override
	public String toString() {
		return theState + " " + (getValue() ? "active" : "inactive") + (theCause == null ? "" : "(" + theCause + ")");
	}
}

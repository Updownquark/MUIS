package org.muis.core.mgr;

import java.util.concurrent.ConcurrentHashMap;

import org.muis.core.event.MuisEvent;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine {
	private static final String INACTIVE = "inactive";

	/** Allows control over one state in an engine */
	public interface StateController {
		/** @return The engine that this controller controls a state in */
		StateEngine getEngine();

		/** @return The state that this controller controls */
		String getState();

		/** @return Whether this controller's state is active in the engine at the moment */
		boolean isActive();

		/**
		 * @param active Whether this controller's state should be active in the engine now
		 * @param cause The event that caused the change--may be null, but should be provided if possible
		 */
		void setActive(boolean active, MuisEvent<?> cause);
	}

	/** Allows notification when states change in an engine */
	public interface StateListener {
		/**
		 * @param state The state just entered
		 * @param cause The event that caused the change--may be null
		 */
		void entered(String state, MuisEvent<?> cause);

		/**
		 * @param state The state just exited
		 * @param cause The event that caused the change--may be null
		 */
		void exited(String state, MuisEvent<?> cause);
	}

	private final ConcurrentHashMap<String, String> theStates;

	private StateControllerImpl [] theStateControllers;

	private Object theStateControllerLock;

	private final prisms.arch.event.ListenerManager<StateListener> theListeners;

	/** Creates a state engine */
	public StateEngine() {
		theStates = new ConcurrentHashMap<>();
		theStateControllers = new StateControllerImpl[0];
		theStateControllerLock = new Object();
		theListeners = new prisms.arch.event.ListenerManager<>(StateListener.class);
	}

	/**
	 * @param state The state to check
	 * @return Whether the given state is currently active in this engine
	 */
	public boolean is(String state) {
		return isActive(theStates.get(state));
	}

	private static boolean isActive(String stateValue) {
		return stateValue != null && stateValue != INACTIVE;
	}

	/**
	 * @param state The state to listen for, or null to receive notification when any state changes
	 * @param listener The listener to notify when the given state (or any state if {@code state} is null) changes
	 */
	public void addListener(String state, StateListener listener) {
		if(state == null)
			theListeners.addListener(listener);
		else
			theListeners.addListener(state, listener);
	}

	/**
	 * @param state The state to remove the listener for
	 * @param listener The listener to remove from listening for changes to the given state
	 */
	public void removeListener(String state, StateListener listener) {
		theListeners.removeListener(state, listener);
	}

	/** @param listener The listener to stop listening with */
	public void removeListener(StateListener listener) {
		theListeners.removeListener(listener);
	}

	/**
	 * @param state The state to add to this engine
	 * @return A controller which allows the caller to set whether the state is active
	 * @throws IllegalArgumentException If the given state is already controlled by another controller in this engine
	 */
	public StateController addState(String state) throws IllegalArgumentException {
		if(state == null)
			throw new NullPointerException("state cannot be null");
		StateControllerImpl ret;
		synchronized(theStateControllerLock) {
			for(StateControllerImpl ctrlr : theStateControllers)
				if(ctrlr.getState().equals(state))
					throw new IllegalArgumentException("The state \"" + state + "\" is already controlled in this engine");
			ret = new StateControllerImpl(state);
			theStateControllers = prisms.util.ArrayUtils.add(theStateControllers, ret);
		}
		return ret;
	}

	private void stateChanged(String state, final boolean active, MuisEvent<?> event) {
		String old = theStates.put(state, active ? state : INACTIVE);
		if(isActive(old) == active)
			return;
		StateListener [] listeners = theListeners.getListeners(state);
		for(StateListener listener : listeners) {
			if(active)
				listener.entered(state, event);
			else
				listener.exited(state, event);
		}
	}

	private class StateControllerImpl implements StateController {
		private final String theState;

		public StateControllerImpl(String state) {
			theState = state;
		}

		@Override
		public StateEngine getEngine() {
			return StateEngine.this;
		}

		@Override
		public String getState() {
			return theState;
		}

		@Override
		public boolean isActive() {
			return is(theState);
		}

		@Override
		public void setActive(boolean active, MuisEvent<?> cause) {
			stateChanged(theState, active, cause);
		}
	}
}

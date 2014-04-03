package org.muis.core.mgr;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.muis.core.event.MuisEvent;

import prisms.util.ArrayUtils;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine implements StateSet {
	/** Allows control over one state in an engine */
	public interface StateController {
		/** @return The engine that this controller controls a state in */
		StateEngine getEngine();

		/** @return The state that this controller controls */
		MuisState getState();

		/** @return Whether this controller's state is active in the engine at the moment */
		boolean isActive();

		/**
		 * @param active Whether this controller's state should be active in the engine now
		 * @param cause The event that caused the change--may be null, but should be provided if possible
		 */
		void setActive(boolean active, MuisEvent cause);
	}

	/** Allows notification when states change in an engine */
	public interface StateListener {
		/**
		 * @param state The state just entered
		 * @param cause The event that caused the change--may be null
		 */
		void entered(MuisState state, MuisEvent cause);

		/**
		 * @param state The state just exited
		 * @param cause The event that caused the change--may be null
		 */
		void exited(MuisState state, MuisEvent cause);
	}

	private static class StateValue {
		private final boolean isActive;

		private AtomicInteger theStackChecker;

		StateValue(boolean active, AtomicInteger stackChecker) {
			isActive = active;
			theStackChecker = stackChecker;
		}

		StateValue(boolean active) {
			isActive = active;
		}

		boolean isActive() {
			return isActive;
		}

		AtomicInteger getStackChecker() {
			if(theStackChecker == null)
				theStackChecker = new AtomicInteger();
			return theStackChecker;
		}

		void setStackChecker(AtomicInteger stackChecker) {
			theStackChecker = stackChecker;
		}
	}

	private final ConcurrentHashMap<MuisState, StateValue> theStates;

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

	@Override
	public boolean is(MuisState state) {
		return isActive(theStates.get(state));
	}

	private static boolean isActive(StateValue stateValue) {
		return stateValue != null && stateValue.isActive();
	}

	/**
	 * @param state The state to check
	 * @return Whether the given state is controller in this state engine
	 */
	public boolean recognizes(MuisState state) {
		for(StateController control : theStateControllers)
			if(control.getState().equals(state))
				return true;
		return false;
	}

	/**
	 * @param stateName The state name to check
	 * @return The state controlled by this engine with the given name, or null if this engine does not control a state with the given name
	 */
	public MuisState getState(String stateName) {
		for(StateController control : theStateControllers)
			if(control.getState().getName().equals(stateName))
				return control.getState();
		return null;
	}

	@Override
	public Iterator<MuisState> iterator() {
		return ArrayUtils.conditionalIterator(theStates.entrySet().iterator(),
			new ArrayUtils.Accepter<java.util.Map.Entry<MuisState, StateValue>, MuisState>() {
				@Override
				public MuisState accept(Entry<MuisState, StateValue> value) {
					if(!isActive(value.getValue()))
						return null;
					return value.getKey();
				}
			}, false);
	}

	@Override
	public MuisState [] toArray() {
		java.util.ArrayList<MuisState> ret = new java.util.ArrayList<>();
		for(java.util.Map.Entry<MuisState, StateValue> entry : theStates.entrySet()) {
			if(isActive(entry.getValue()))
				ret.add(entry.getKey());
		}
		return ret.toArray(new MuisState[ret.size()]);
	}

	/** @return All states that are controlled in this engine, whether they are active or not */
	public Iterable<MuisState> getAllStates() {
		return new Iterable<MuisState>() {
			@Override
			public Iterator<MuisState> iterator() {
				return new Iterator<MuisState>() {
					private final StateController [] theControllerSnapshot = theStateControllers;

					private int theIndex;

					@Override
					public boolean hasNext() {
						return theIndex < theControllerSnapshot.length;
					}

					@Override
					public MuisState next() {
						return theControllerSnapshot[theIndex++].getState();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * @param state The state to listen for, or null to receive notification when any state changes
	 * @param listener The listener to notify when the given state (or any state if {@code state} is null) changes
	 */
	public void addListener(MuisState state, StateListener listener) {
		if(state == null)
			theListeners.addListener(listener);
		else
			theListeners.addListener(state, listener);
	}

	/**
	 * @param state The state to remove the listener for
	 * @param listener The listener to remove from listening for changes to the given state
	 */
	public void removeListener(MuisState state, StateListener listener) {
		theListeners.removeListener(state, listener);
	}

	/** @param listener The listener to stop listening with */
	public void removeListener(StateListener listener) {
		theListeners.removeListener(listener);
	}

	/**
	 * @param state The state to add to this engine
	 * @throws IllegalArgumentException If the given state is already recognized in this engine
	 */
	public void addState(MuisState state) throws IllegalArgumentException {
		if(state == null)
			throw new NullPointerException("state cannot be null");
		if(theStates.containsKey(state))
			throw new IllegalArgumentException("The state \"" + state + "\" is already added to this engine");
		theStates.put(state, new StateValue(false, new AtomicInteger()));
	}

	/**
	 * @param state The state to request control of
	 * @return A controller which allows the caller to set whether the state is active
	 * @throws IllegalArgumentException If the given state is not recognized or is already controlled by another controller in this engine
	 */
	public StateController control(MuisState state) throws IllegalArgumentException {
		if(state == null)
			throw new NullPointerException("state cannot be null");
		if(!theStates.containsKey(state))
			throw new IllegalArgumentException("The state \"" + state + "\" is not recognized in this engine");
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

	private void stateChanged(MuisState state, final boolean active, MuisEvent event) {
		StateValue newState = new StateValue(active);
		StateValue old = theStates.put(state, newState);
		newState.setStackChecker(old.getStackChecker());
		int stack = newState.getStackChecker().incrementAndGet();
		if(old.isActive() == active)
			return;
		StateListener [] listeners = theListeners.getListeners(state);
		for(StateListener listener : listeners) {
			if(stack != newState.getStackChecker().get())
				break;
			if(active)
				listener.entered(state, event);
			else
				listener.exited(state, event);
		}
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for(MuisState state : this) {
			if(ret.length() > 1)
				ret.append(", ");
			ret.append(state.getName());
		}
		ret.append('}');
		return ret.toString();
	}

	private class StateControllerImpl implements StateController {
		private final MuisState theState;

		public StateControllerImpl(MuisState state) {
			theState = state;
		}

		@Override
		public StateEngine getEngine() {
			return StateEngine.this;
		}

		@Override
		public MuisState getState() {
			return theState;
		}

		@Override
		public boolean isActive() {
			return is(theState);
		}

		@Override
		public void setActive(boolean active, MuisEvent cause) {
			stateChanged(theState, active, cause);
		}
	}
}

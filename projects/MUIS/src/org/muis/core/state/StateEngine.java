package org.muis.core.state;

import java.util.concurrent.ConcurrentHashMap;

import org.muis.core.event.MuisEvent;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine
{
	/** Allows control over one state in an engine */
	public interface StateController
	{
		StateEngine getEngine();

		String getState();

		boolean isActive();

		void setActive(boolean active, MuisEvent<?> cause);
	}

	/** Allows notification when states change in an engine */
	public interface StateListener
	{
		void entered(String state, MuisEvent cause);

		void exited(String state, MuisEvent cause);
	}

	private final ConcurrentHashMap<String, String> theStates;

	private StateControllerImpl [] theStateControllers;

	private Object theStateControllerLock;

	private final prisms.arch.event.ListenerManager<StateListener> theListeners;

	/** Creates a state engine */
	public StateEngine()
	{
		theStates = new ConcurrentHashMap<>();
		theStateControllers = new StateControllerImpl[0];
		theStateControllerLock = new Object();
		theListeners = new prisms.arch.event.ListenerManager<>(StateListener.class);
	}

	/**
	 * @param state The state to check
	 * @return Whether the given state is currently active in this engine
	 */
	public boolean is(String state)
	{
		return theStates.containsKey(state);
	}

	/**
	 * @param state The state to listen for, or null to receive notification when any state changes
	 * @param listener The listener to notify when the given state (or any state if {@code state} is null) changes
	 */
	public void addListener(String state, StateListener listener)
	{
		if(state == null)
			theListeners.addListener(listener);
		else
			theListeners.addListener(state, listener);
	}

	public void removeListener(String state, StateListener listener)
	{
		theListeners.removeListener(state, listener);
	}

	public void removeListener(StateListener listener)
	{
		theListeners.removeListener(listener);
	}

	/**
	 * @param state The state to add to this engine
	 * @return A controller which allows the caller to set whether the state is active
	 * @throws IllegalArgumentException If the given state is already controlled by another controller in this engine
	 */
	public StateController addState(String state) throws IllegalArgumentException
	{
		if(state == null)
			throw new NullPointerException("state cannot be null");
		StateControllerImpl ret;
		synchronized(theStateControllerLock)
		{
			for(StateControllerImpl ctrlr : theStateControllers)
				if(ctrlr.getState().equals(state))
					throw new IllegalArgumentException("The state \"" + state + "\" is already controlled in this engine");
			ret = new StateControllerImpl(state);
			theStateControllers = prisms.util.ArrayUtils.add(theStateControllers, ret);
		}
		return ret;
	}

	private void stateChanged(String state, final boolean active, MuisEvent<?> event)
	{
		String old = theStates.put(state, active ? state : null);
		if((old != null) == active)
			return;
		StateListener [] listeners = theListeners.getListeners(state);
		for(StateListener listener : listeners)
		{
			if(active)
				listener.entered(state, event);
			else
				listener.exited(state, event);
		}
	}

	private class StateControllerImpl implements StateController
	{
		private final String theState;

		public StateControllerImpl(String state)
		{
			theState = state;
		}

		@Override
		public StateEngine getEngine()
		{
			return StateEngine.this;
		}

		@Override
		public String getState()
		{
			return theState;
		}

		@Override
		public boolean isActive()
		{
			return is(theState);
		}

		@Override
		public void setActive(boolean active, MuisEvent<?> cause)
		{
			stateChanged(theState, active, cause);
		}
	}
}

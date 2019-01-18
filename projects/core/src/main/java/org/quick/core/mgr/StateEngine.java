package org.quick.core.mgr;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.util.TypeTokens;
import org.qommons.Transaction;
import org.qommons.collect.RRWLockingStrategy;
import org.qommons.tree.BetterTreeList;
import org.quick.core.QuickElement;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine implements StateSet {
	/** Allows control over one state in an engine */
	public interface StateController {
		/** @return The engine that this controller controls a state in */
		StateEngine getEngine();

		/** @return The state that this controller controls */
		QuickState getState();

		/** @return Whether the state is currently active */
		boolean isActive();

		/**
		 * @param active Whether the state should be active
		 * @param cause The cause of the change
		 * @return Whether the state was previously active
		 */
		boolean setActive(boolean active, Object cause);
	}

	private final QuickElement theElement;
	private final ConcurrentHashMap<QuickState, StateHolder> theStateHolders;
	private final ObservableSet<QuickState> theStateSet;
	private final ObservableSet<QuickState> theExposedStateSet;
	private final ObservableSet<QuickState> theActiveStates;
	private final ObservableSet<QuickState> theExposedActiveStates;

	/**
	 * Creates a state engine
	 *
	 * @param element The element that this engine keeps state for
	 */
	public StateEngine(QuickElement element) {
		theElement = element;
		theStateHolders = new ConcurrentHashMap<>();
		theStateSet = ObservableCollection
			.create(TypeTokens.get().of(QuickState.class), new BetterTreeList<>(new RRWLockingStrategy(theElement.getAttributeLocker())))
			.flow().distinct().collect();
		theActiveStates = ObservableCollection
			.create(TypeTokens.get().of(QuickState.class), new BetterTreeList<>(new RRWLockingStrategy(theElement.getAttributeLocker())))
			.flow().distinct().collect();
		theExposedStateSet = theStateSet.flow().unmodifiable(false).collectPassive();
		theExposedActiveStates = theActiveStates.flow().unmodifiable(false).collectPassive();
	}

	@Override
	public boolean is(QuickState state) {
		return theActiveStates.contains(state);
	}

	@Override
	public ObservableValue<Boolean> observe(QuickState state) {
		return theActiveStates.observeContains(ObservableValue.of(state));
	}

	/**
	 * @param state The state to check
	 * @return Whether the given state is controller in this state engine
	 */
	public boolean recognizes(QuickState state) {
		StateHolder holder = theStateHolders.get(state);
		return holder != null && holder.theController.get() != null;
	}

	/**
	 * @param stateName The state name to check
	 * @return The state controlled by this engine with the given name, or null if this engine does not control a state with the given name
	 */
	public QuickState getState(String stateName) {
		for (QuickState state : theStateHolders.keySet())
			if (state.getName().equals(stateName))
				return state;
		return null;
	}

	@Override
	public Iterator<QuickState> iterator() {
		return theExposedActiveStates.iterator();
	}

	@Override
	public QuickState [] toArray() {
		return theExposedActiveStates.toArray();
	}

	@Override
	public ObservableSet<QuickState> allStates() {
		return theExposedStateSet;
	}

	@Override
	public ObservableSet<QuickState> activeStates() {
		return theExposedActiveStates;
	}

	/**
	 * @param state The state to add to this engine
	 * @throws IllegalArgumentException If the given state is already recognized in this engine
	 */
	public void addState(QuickState state) throws IllegalArgumentException {
		if(state == null)
			throw new NullPointerException("state cannot be null");
		StateHolder holder = theStateHolders.get(state);
		if (holder != null)
			throw new IllegalArgumentException("The state \"" + state + "\" is already added to this engine");
		holder = new StateHolder(state);
		theStateHolders.put(state, holder);
		theStateSet.add(state);
	}

	/**
	 * @param state The state to request control of
	 * @return A controller which allows the caller to set whether the state is active
	 * @throws IllegalArgumentException If the given state is not recognized or is already controlled by another controller in this engine
	 */
	public StateController control(QuickState state) throws IllegalArgumentException {
		if(state == null)
			throw new NullPointerException("state cannot be null");
		StateHolder holder = theStateHolders.get(state);
		if (holder == null)
			throw new IllegalArgumentException("The state \"" + state + "\" is not recognized in this engine");
		StateControllerImpl ret = new StateControllerImpl(holder);
		if (!holder.theController.compareAndSet(null, ret))
			throw new IllegalArgumentException("The state \"" + state + "\" is already controlled in this engine");
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for (QuickState state : theActiveStates) {
			if(ret.length() > 1)
				ret.append(", ");
			ret.append(state.getName());
		}
		ret.append('}');
		return ret.toString();
	}

	private class StateHolder {
		final QuickState theState;
		private final AtomicReference<StateControllerImpl> theController;

		StateHolder(QuickState state) {
			theState = state;
			theController = new AtomicReference<>();
		}

		QuickState getState() {
			return theState;
		}
	}

	private class StateControllerImpl implements StateController {
		private final StateHolder theState;
		private boolean isActive;

		public StateControllerImpl(StateHolder state) {
			theState = state;
		}

		@Override
		public StateEngine getEngine() {
			return StateEngine.this;
		}

		@Override
		public QuickState getState() {
			return theState.getState();
		}

		@Override
		public boolean isActive() {
			return theActiveStates.contains(theState.theState);
		}

		@Override
		public boolean setActive(boolean active, Object cause) {
			if (isActive == active)
				return active;
			else {
				try (Transaction t = theActiveStates.lock(true, cause)) {
					if (active)
						return !theActiveStates.add(theState.theState);
					else
						return theActiveStates.remove(theState.theState);
				}
			}
		}
	}
}

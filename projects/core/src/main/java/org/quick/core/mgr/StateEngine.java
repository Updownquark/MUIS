package org.quick.core.mgr;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.observe.*;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableHashSet;
import org.qommons.Transaction;
import org.quick.core.QuickElement;

import com.google.common.reflect.TypeToken;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine implements StateSet {
	/** Allows control over one state in an engine */
	public interface StateController extends SettableValue<Boolean> {
		/** @return The engine that this controller controls a state in */
		StateEngine getEngine();

		/** @return The state that this controller controls */
		QuickState getState();
	}

	@SuppressWarnings("unused")
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
		theStateSet = new ObservableHashSet<>(TypeToken.of(QuickState.class));
		theActiveStates = new ObservableHashSet<>(theStateSet.getType());
		theExposedStateSet = theStateSet.immutable();
		theExposedActiveStates = theActiveStates.immutable();
	}

	@Override
	public boolean is(QuickState state) {
		return theActiveStates.contains(state);
	}

	@Override
	public ObservableValue<Boolean> observe(QuickState state) {
		StateHolder holder = theStateHolders.get(state);
		if (holder != null)
			return holder;
		else
			return ObservableValue.constant(TypeToken.of(Boolean.TYPE), false);
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

	private class StateHolder implements ObservableValue<Boolean> {
		private final QuickState theState;
		private final AtomicReference<StateControllerImpl> theController;

		StateHolder(QuickState state) {
			theState = state;
			theController = new AtomicReference<>();
		}

		QuickState getState() {
			return theState;
		}

		@Override
		public TypeToken<Boolean> getType() {
			return TypeToken.of(Boolean.TYPE);
		}

		@Override
		public boolean isSafe() {
			return theActiveStates.isSafe();
		}

		@Override
		public Boolean get() {
			return theActiveStates.contains(theState);
		}

		@Override
		public Subscription subscribe(Observer<? super ObservableValueEvent<Boolean>> observer) {
			Subscription sub = theActiveStates.changes().act(event -> {
				switch (event.type) {
				case add:
					if (event.values.contains(theState))
						observer.onNext(createChangeEvent(false, true, event));
					break;
				case remove:
					if (event.values.contains(theState))
						observer.onNext(createChangeEvent(true, false, event));
					break;
				case set:
					if (event.values.contains(theState)) {
						if (!event.oldValues.contains(theState))
							observer.onNext(createChangeEvent(false, true, event));
					} else if (event.oldValues.contains(theState))
						observer.onNext(createChangeEvent(true, false, event));
					break;
				}
			});
			observer.onNext(createInitialEvent(theActiveStates.contains(theState)));
			return sub;
		}
	}

	private class StateControllerImpl implements StateController {
		private final StateHolder theState;

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
		public TypeToken<Boolean> getType() {
			return TypeToken.of(Boolean.class);
		}

		@Override
		public boolean isSafe() {
			return theState.isSafe();
		}

		@Override
		public Boolean get() {
			return theState.get();
		}

		@Override
		public Subscription subscribe(Observer<? super ObservableValueEvent<Boolean>> observer) {
			return theState.subscribe(observer);
		}

		@Override
		public Boolean set(Boolean active, Object cause) throws IllegalArgumentException {
			if (active == null)
				throw new IllegalArgumentException("A null boolean is not allowed");
			Transaction trans = cause != null ? theActiveStates.lock(true, cause) : null;
			try {
				if (active)
					return !theActiveStates.add(getState());
				else
					return theActiveStates.remove(getState());
			} finally {
				if (trans != null)
					trans.close();
			}
		}

		@Override
		public String isAcceptable(Boolean value) {
			return value != null ? null : "null Boolean value unacceptable";
		}

		@Override
		public ObservableValue<String> isEnabled() {
			return ObservableValue.constant(TypeToken.of(String.class), null);
		}
	}
}

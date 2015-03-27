package org.muis.core.mgr;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.StateChangedEvent;
import org.observe.*;
import org.observe.collect.DefaultObservableSet;
import org.observe.collect.ObservableSet;

import prisms.lang.Type;
import prisms.util.ArrayUtils;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine extends DefaultObservable<StateChangedEvent> implements StateSet {
	/** Allows control over one state in an engine */
	public interface StateController extends SettableValue<Boolean> {
		/** @return The engine that this controller controls a state in */
		StateEngine getEngine();

		/** @return The state that this controller controls */
		MuisState getState();
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

	private final MuisElement theElement;
	private final ConcurrentHashMap<MuisState, StateValue> theStates;
	private final ObservableSet<MuisState> theStateSet;
	private final ObservableSet<MuisState> theActiveStates;
	private final java.util.Set<MuisState> theStateSetController;
	private final java.util.Set<MuisState> theActiveStateController;

	private StateControllerImpl [] theStateControllers;

	private Object theStateControllerLock;

	private Observer<StateChangedEvent> theObservableController;

	/**
	 * Creates a state engine
	 *
	 * @param element The element that this engine keeps state for
	 */
	public StateEngine(MuisElement element) {
		theObservableController = super.control(null);
		theElement = element;
		theStates = new ConcurrentHashMap<>();
		theStateControllers = new StateControllerImpl[0];
		theStateControllerLock = new Object();

		DefaultObservableSet<MuisState> allStates = new DefaultObservableSet<>(new Type(MuisState.class));
		theStateSetController = allStates.control(null);
		theStateSet = new org.observe.util.ObservableSetWrapper<MuisState>(allStates) {
			@Override
			public String toString() {
				return "allStates(" + theElement.getTagName() + ")";
			}
		};
		DefaultObservableSet<MuisState> activeStates = new DefaultObservableSet<>(theStateSet.getType());
		theActiveStateController = activeStates.control(null);
		theActiveStates = new org.observe.util.ObservableSetWrapper<MuisState>(activeStates) {
			@Override
			public String toString() {
				return "activeStates(" + theElement.getTagName() + ")=" + super.toString();
			}
		};
	}

	@Override
	public boolean is(MuisState state) {
		return isActive(theStates.get(state));
	}

	@Override
	public ObservableValue<Boolean> observe(MuisState state) {
		for(StateController control : theStateControllers)
			if(control.getState().equals(state))
				return control.unsettable();
		return ObservableValue.constant(null, false);
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
		return ArrayUtils.conditionalIterator(theStates.entrySet().iterator(), value -> {
			if(!isActive(value.getValue()))
				return null;
			return value.getKey();
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
		return () -> {
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
		};
	}

	@Override
	public ObservableSet<MuisState> allStates() {
		return theStateSet;
	}

	@Override
	public ObservableSet<MuisState> activeStates() {
		return theActiveStates;
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
		theStateSetController.add(state);
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

	private void stateChanged(MuisState state, final boolean active, Object cause) {
		final StateValue newState = new StateValue(active);
		StateValue old = theStates.put(state, newState);
		newState.setStackChecker(old.getStackChecker());
		final int stack = newState.getStackChecker().incrementAndGet();
		if(old.isActive() == active)
			return;

		MuisEvent event = cause instanceof MuisEvent ? (MuisEvent) cause : null;
		StateChangedEvent sce = new StateChangedEvent(theElement, state, active, event) {
			@Override
			public boolean isOverridden() {
				return stack != newState.getStackChecker().get();
			}
		};
		theObservableController.onNext(sce);
		if(active)
			theActiveStateController.add(state);
		else
			theActiveStateController.remove(state);
		theElement.events().fire(sce);
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

	private class StateControllerImpl extends DefaultSettableValue<Boolean> implements StateController {
		private final MuisState theState;

		private final Observer<ObservableValueEvent<Boolean>> theController;

		public StateControllerImpl(MuisState state) {
			theState = state;
			theController = control(null);
			StateEngine.this.filter(event -> event.getState().equals(theState)).act(event -> theController.onNext(event));
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
		public SettableValue<Boolean> set(Boolean active, Object cause) throws IllegalArgumentException {
			stateChanged(theState, active, cause);
			return this;
		}

		@Override
		public String isAcceptable(Boolean value) {
			return value != null ? null : "null Boolean value unacceptable";
		}

		@Override
		public ObservableValue<Boolean> isEnabled() {
			return ObservableValue.constant(true);
		}

		@Override
		public Type getType() {
			return new Type(Boolean.class);
		}

		@Override
		public Boolean get() {
			return is(theState);
		}
	}
}

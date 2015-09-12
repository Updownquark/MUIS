package org.quick.core.mgr;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.observe.*;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableHashSet;
import org.quick.core.QuickElement;
import org.quick.core.event.QuickEvent;
import org.quick.core.event.StateChangedEvent;

import prisms.lang.Type;
import prisms.util.ArrayUtils;

/** Keeps track of states for an entity and fires events when they change */
public class StateEngine extends DefaultObservable<StateChangedEvent> implements StateSet {
	/** Allows control over one state in an engine */
	public interface StateController extends SettableValue<Boolean> {
		/** @return The engine that this controller controls a state in */
		StateEngine getEngine();

		/** @return The state that this controller controls */
		QuickState getState();
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

	private final QuickElement theElement;
	private final ConcurrentHashMap<QuickState, StateValue> theStates;
	private final ObservableSet<QuickState> theStateSet;
	private final ObservableSet<QuickState> theActiveStates;
	private final java.util.Set<QuickState> theStateSetController;
	private final java.util.Set<QuickState> theActiveStateController;

	private StateControllerImpl [] theStateControllers;

	private Object theStateControllerLock;

	private Observer<StateChangedEvent> theObservableController;

	/**
	 * Creates a state engine
	 *
	 * @param element The element that this engine keeps state for
	 */
	public StateEngine(QuickElement element) {
		theObservableController = super.control(null);
		theElement = element;
		theStates = new ConcurrentHashMap<>();
		theStateControllers = new StateControllerImpl[0];
		theStateControllerLock = new Object();

		ObservableHashSet<QuickState> allStates = new ObservableHashSet<>(new Type(QuickState.class));
		theStateSetController = allStates;
		theStateSet = new org.observe.util.ObservableSetWrapper<QuickState>(allStates, false) {
			@Override
			public String toString() {
				return "allStates(" + theElement.getTagName() + ")";
			}
		};
		ObservableHashSet<QuickState> activeStates = new ObservableHashSet<>(theStateSet.getType());
		theActiveStateController = activeStates;
		theActiveStates = new org.observe.util.ObservableSetWrapper<QuickState>(activeStates, false) {
			@Override
			public String toString() {
				return "activeStates(" + theElement.getTagName() + ")=" + super.toString();
			}
		};
	}

	@Override
	public boolean is(QuickState state) {
		return isActive(theStates.get(state));
	}

	@Override
	public ObservableValue<Boolean> subscribe(QuickState state) {
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
	public boolean recognizes(QuickState state) {
		for(StateController control : theStateControllers)
			if(control.getState().equals(state))
				return true;
		return false;
	}

	/**
	 * @param stateName The state name to check
	 * @return The state controlled by this engine with the given name, or null if this engine does not control a state with the given name
	 */
	public QuickState getState(String stateName) {
		for(StateController control : theStateControllers)
			if(control.getState().getName().equals(stateName))
				return control.getState();
		return null;
	}

	@Override
	public Iterator<QuickState> iterator() {
		return ArrayUtils.conditionalIterator(theStates.entrySet().iterator(), value -> {
			if(!isActive(value.getValue()))
				return null;
			return value.getKey();
		}, false);
	}

	@Override
	public QuickState [] toArray() {
		java.util.ArrayList<QuickState> ret = new java.util.ArrayList<>();
		for(java.util.Map.Entry<QuickState, StateValue> entry : theStates.entrySet()) {
			if(isActive(entry.getValue()))
				ret.add(entry.getKey());
		}
		return ret.toArray(new QuickState[ret.size()]);
	}

	/** @return All states that are controlled in this engine, whether they are active or not */
	public Iterable<QuickState> getAllStates() {
		return () -> {
			return new Iterator<QuickState>() {
				private final StateController [] theControllerSnapshot = theStateControllers;

				private int theIndex;

				@Override
				public boolean hasNext() {
					return theIndex < theControllerSnapshot.length;
				}

				@Override
				public QuickState next() {
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
	public ObservableSet<QuickState> allStates() {
		return theStateSet;
	}

	@Override
	public ObservableSet<QuickState> activeStates() {
		return theActiveStates;
	}

	/**
	 * @param state The state to add to this engine
	 * @throws IllegalArgumentException If the given state is already recognized in this engine
	 */
	public void addState(QuickState state) throws IllegalArgumentException {
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
	public StateController control(QuickState state) throws IllegalArgumentException {
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

	private boolean stateChanged(QuickState state, final boolean active, Object cause) {
		final StateValue newState = new StateValue(active);
		StateValue old = theStates.put(state, newState);
		newState.setStackChecker(old.getStackChecker());
		final int stack = newState.getStackChecker().incrementAndGet();
		if(old.isActive() == active)
			return old.isActive();

		QuickEvent event = cause instanceof QuickEvent ? (QuickEvent) cause : null;
		StateChangedEvent sce = new StateChangedEvent(theElement, state, false, active, event) {
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
		return !active;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for(QuickState state : this) {
			if(ret.length() > 1)
				ret.append(", ");
			ret.append(state.getName());
		}
		ret.append('}');
		return ret.toString();
	}

	private class StateControllerImpl extends DefaultSettableValue<Boolean> implements StateController {
		private final QuickState theState;

		private final Observer<ObservableValueEvent<Boolean>> theController;

		public StateControllerImpl(QuickState state) {
			theState = state;
			theController = control(null);
			StateEngine.this.filter(event -> event.getState().equals(theState)).act(event -> theController.onNext(event));
		}

		@Override
		public StateEngine getEngine() {
			return StateEngine.this;
		}

		@Override
		public QuickState getState() {
			return theState;
		}

		@Override
		public Boolean set(Boolean active, Object cause) throws IllegalArgumentException {
			return stateChanged(theState, active, cause);
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

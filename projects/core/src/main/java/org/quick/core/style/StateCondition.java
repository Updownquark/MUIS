package org.quick.core.style;

import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.observe.*;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickState;

import com.google.common.reflect.TypeToken;

/** A condition on an {@link QuickElement element's} active states */
public abstract class StateCondition implements Comparable<StateCondition> {
	/** A state expression that depends on any number of other expressions */
	public static abstract class StateCollectionExpression extends StateCondition implements Iterable<StateCondition> {
		private final StateCondition[] theWrapped;

		/** @param wrapped The state expressions that this expression uses to evaluate */
		protected StateCollectionExpression(StateCondition... wrapped) {
			theWrapped = new StateCondition[wrapped.length];
			System.arraycopy(wrapped, 0, theWrapped, 0, wrapped.length);
			init();
		}

		/** @return The number of children in this state expression collection */
		public int getChildCount() {
			return theWrapped.length;
		}

		/**
		 * @param index The index of the child to get
		 * @return The child in this state expression collection at the given index
		 */
		public StateCondition getChild(int index) {
			return theWrapped[index];
		}

		@Override
		public java.util.Iterator<StateCondition> iterator() {
			return org.qommons.IterableUtils.iterator(theWrapped, true);
		}

		@Override
		protected int initPriority() {
			int ret = 0;
			for (StateCondition exp : this)
				ret += exp.getPriority();
			return ret;
		}

		@Override
		protected int recursiveCompare(StateCondition o) {
			int comp = super.recursiveCompare(o);
			if (comp != 0)
				return comp;
			StateCollectionExpression sce = (StateCollectionExpression) o;
			int i;
			for (i = 0; i < theWrapped.length && i < sce.theWrapped.length; i++) {
				comp = theWrapped[i].compareTo(sce.theWrapped[i]);
				if (comp != 0)
					return comp;
			}
			if (i < theWrapped.length)
				return 1;
			else if (i < sce.theWrapped.length)
				return -1;
			return 0;
		}

		/** @return This collection's set of unique children */
		protected StateCondition[] getWrappedChildren() {
			SortedSet<StateCondition> ret = new TreeSet<>();
			for (StateCondition wrapped : theWrapped) {
				add(wrapped, ret);
			}
			return ret.toArray(new StateCondition[ret.size()]);
		}

		private void add(StateCondition expr, SortedSet<StateCondition> ret) {
			if (expr.getClass() == getClass()) {
				for (StateCondition sub : ((StateCollectionExpression) expr).theWrapped)
					add(sub, ret);
			} else {
				expr = expr.getUnique();
				if (expr == null)
					return;
				else
					ret.add(expr);
			}
		}

		@Override
		public boolean equals(Object o) {
			return o != null && getClass() == o.getClass()
				&& org.qommons.ArrayUtils.equals(theWrapped, ((StateCollectionExpression) o).theWrapped);
		}

		@Override
		public int hashCode() {
			int ret = 13;
			for (StateCondition expr : this) {
				ret = ret * 13 + expr.hashCode();
			}
			return ret;
		}
	}

	/** An expression that is true if and only if every one of a set of expressions are true */
	public static class And extends StateCollectionExpression {
		/** @param wrapped The state expressions that this expression will use to evaluate */
		public And(StateCondition... wrapped) {
			super(wrapped);
		}

		@Override
		public boolean matches(Set<QuickState> states) {
			for (StateCondition exp : this)
				if (!exp.matches(states))
					return false;
			return true;
		}

		@Override
		public StateCondition getUnique() {
			StateCondition[] wrapped = getWrappedChildren();
			if (wrapped.length == 0)
				return null;
			else if (wrapped.length == 1)
				return wrapped[0];
			ArrayList<Or> ors = new ArrayList<>();
			ArrayList<StateCondition> ands = new ArrayList<>();
			for (StateCondition wrap : wrapped)
				add(wrap, ors, ands);
			if (ors.isEmpty()) {
				int i;
				boolean equal = true;
				java.util.Iterator<StateCondition> iter = iterator();
				for (i = 0; i < wrapped.length && iter.hasNext(); i++)
					if (!iter.next().equals(wrapped[i])) {
						equal = false;
						break;
					}
				if (equal)
					return this;
				return new And(wrapped);
			}
			// Bubble up the ors to the top level
			int[] orIdxes = new int[ors.size()];
			SortedSet<And> retCh = new TreeSet<>();
			SortedSet<StateCondition> andCh = new TreeSet<>();
			while (true) {
				andCh.clear();
				for (int i = 0; i < ors.size(); i++)
					andCh.add(ors.get(i).getChild(orIdxes[i]));
				for (StateCondition and : ands)
					andCh.add(and.getUnique());
				retCh.add(new And(andCh.toArray(new StateCondition[andCh.size()])));
				int orIdx = orIdxes.length - 1;
				do {
					orIdxes[orIdx]++;
					if (orIdxes[orIdx] == ors.get(orIdx).getChildCount()) {
						orIdxes[orIdx] = 0;
						orIdx--;
					}
				} while (orIdx >= 0);
				if (orIdx < 0)
					break;
			}
			return new Or(retCh.toArray(new StateCondition[retCh.size()]));
		}

		private void add(StateCondition expr, ArrayList<Or> ors, ArrayList<StateCondition> ands) {
			if (expr instanceof Or)
				ors.add((Or) expr);
			else
				ands.add(expr);
		}

		@Override
		protected int getTypePriority() {
			return 3;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append('(');
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0)
					ret.append(" & ");
				ret.append(getChild(i));
			}
			ret.append(')');
			return ret.toString();
		}
	}

	/** An expression that is true if at least one of a set of expressions is true */
	public static class Or extends StateCollectionExpression {
		/** @param wrapped The state expressions that this expression will use to evaluate */
		public Or(StateCondition... wrapped) {
			super(wrapped);
		}

		@Override
		public boolean matches(Set<QuickState> states) {
			for (StateCondition exp : this)
				if (exp.matches(states))
					return true;
			return false;
		}

		@Override
		public StateCondition getUnique() {
			StateCondition[] wrapped = getWrappedChildren();
			if (wrapped.length == 0)
				return null;
			else if (wrapped.length == 1)
				return wrapped[0];
			int i;
			boolean equal = true;
			java.util.Iterator<StateCondition> iter = iterator();
			for (i = 0; i < wrapped.length && iter.hasNext(); i++)
				if (!iter.next().equals(wrapped[i])) {
					equal = false;
					break;
				}
			if (equal)
				return this;
			return new Or(wrapped);
		}

		@Override
		protected int getTypePriority() {
			return 2;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append('(');
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0)
					ret.append(" | ");
				ret.append(getChild(i));
			}
			ret.append(')');
			return ret.toString();
		}
	}

	/** A state expression that is the negation of another expression */
	public static class Not extends StateCondition {
		private final StateCondition theWrapped;

		/** @param exp The expression to negate */
		public Not(StateCondition exp) {
			theWrapped = exp;
			init();
		}

		/** @return The state expression that this expression is a negation of */
		public StateCondition getWrapped() {
			return theWrapped;
		}

		@Override
		public boolean matches(Set<QuickState> states) {
			return !theWrapped.matches(states);
		}

		@Override
		public Not getUnique() {
			StateCondition ret = theWrapped.getUnique();
			if (ret == theWrapped)
				return this;
			else if (ret == null)
				return null;
			else
				return ret.not();
		}

		@Override
		protected int initPriority() {
			return theWrapped.getPriority();
		}

		@Override
		protected int recursiveCompare(StateCondition o) {
			int comp = super.recursiveCompare(o);
			if (comp != 0)
				return comp;
			return theWrapped.compareTo(((Not) o).theWrapped);
		}

		@Override
		protected int getTypePriority() {
			return 1;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Not && theWrapped.equals(((Not) o).theWrapped);
		}

		@Override
		public int hashCode() {
			return -theWrapped.hashCode();
		}

		@Override
		public String toString() {
			return "!" + theWrapped;
		}
	}

	/** A state expression that checks whether a single state is active or not */
	public static class Simple extends StateCondition {
		private final QuickState theState;

		/** @param state The state to evaluate on */
		public Simple(QuickState state) {
			theState = state;
			init();
		}

		@Override
		public boolean matches(Set<QuickState> states) {
			return states.contains(theState);
		}

		@Override
		public Simple getUnique() {
			return this;
		}

		@Override
		protected int initPriority() {
			return theState.getPriority();
		}

		@Override
		protected int recursiveCompare(StateCondition o) {
			int comp = super.recursiveCompare(o);
			if (comp != 0)
				return comp;
			comp = theState.getPriority() - ((Simple) o).theState.getPriority();
			if (comp != 0)
				return comp;
			comp = theState.getName().compareToIgnoreCase(((Simple) o).theState.getName());
			if (comp != 0)
				return comp;
			comp = theState.getName().compareTo(((Simple) o).theState.getName());
			return comp;
		}

		@Override
		protected int getTypePriority() {
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Simple && theState.equals(((Simple) o).theState);
		}

		@Override
		public int hashCode() {
			return theState.hashCode();
		}

		@Override
		public String toString() {
			return theState.getName();
		}
	}

	private int thePriority;

	private StateCondition() {
	}

	/** Initializes this state's priority. Should only be called once. */
	protected final void init() {
		thePriority = initPriority();
	}

	/**
	 * @param states The state of the engine to evaluate against
	 * @return Whether this expression returns true for the given state set
	 */
	public abstract boolean matches(Set<QuickState> states);

	/**
	 * @param states The set of states to check against
	 * @return An observable boolean reflecting whether this state condition matches the given set of active states
	 */
	public ObservableValue<Boolean> observeMatches(ObservableSet<QuickState> states) {
		class StateMatchObserver implements ObservableValue<Boolean> {
			@Override
			public TypeToken<Boolean> getType() {
				return TypeToken.of(Boolean.class);
			}

			@Override
			public Boolean get() {
				return matches(states);
			}

			@Override
			public Subscription subscribe(Observer<? super ObservableValueEvent<Boolean>> observer) {
				Subscription sub = states.simpleChanges().act(new Action<Object>() {
					private final AtomicBoolean preMatches = new AtomicBoolean(get());

					{
						boolean initMatch = matches(states);
						preMatches.set(initMatch);
						observer.onNext(createInitialEvent(initMatch));
					}

					@Override
					public void act(Object cause) {
						boolean newMatch = matches(states);
						boolean oldMatch = preMatches.getAndSet(newMatch);
						observer.onNext(createChangeEvent(oldMatch, newMatch, cause));
					}
				});
				observer.onNext(createInitialEvent(get()));
				return sub;
			}

			@Override
			public boolean isSafe() {
				return states.isSafe();
			}
		}
		return new StateMatchObserver();
	}

	/**
	 * @return A state expression that is always {@link #equals(Object) equal} to any state expression that returns the same values from
	 *         {@link #matches(Set)} for every possible set of states
	 */
	public abstract StateCondition getUnique();

	/** @return The priority of this condition, determining its precedence in a {@link StyleSheet} */
	public final int getPriority() {
		return thePriority;
	}

	/**
	 * Calculates the priority of this condition initially
	 *
	 * @return The priority
	 */
	protected abstract int initPriority();

	@Override
	public int compareTo(StateCondition o) {
		int priorityDiff = o.getPriority() - getPriority();
		if (priorityDiff != 0)
			return priorityDiff;
		return getUnique().recursiveCompare(o.getUnique());
	}

	/**
	 * Compares the contents of the two state conditions if their overall priorities are the same
	 *
	 * @param o The state condition to compare against
	 * @return The difference in deep priority between the two conditions
	 */
	protected int recursiveCompare(StateCondition o) {
		int typePriority = getTypePriority();
		int oTypePriority = o.getTypePriority();
		return typePriority - oTypePriority;
	}

	/**
	 * A priority for this condition's type
	 *
	 * @return This condition's type's priority
	 */
	protected abstract int getTypePriority();

	/** @return An expression that is the logical opposite of this expression */
	public Not not() {
		return new Not(this);
	}

	/**
	 * @param exprs The expressions to OR with this expression
	 * @return An expression that is a logical OR of this expression and the given expressions
	 */
	public Or or(StateCondition... exprs) {
		StateCondition[] wrap = new StateCondition[exprs.length + 1];
		wrap[0] = this;
		System.arraycopy(exprs, 0, wrap, 1, exprs.length);
		return new Or(wrap);
	}

	/**
	 * @param exprs The expressions to and with this expression
	 * @return An expression that is a logical AND of this expression and the given expressions
	 */
	public And and(StateCondition... exprs) {
		StateCondition[] wrap = new StateCondition[exprs.length + 1];
		wrap[0] = this;
		System.arraycopy(exprs, 0, wrap, 1, exprs.length);
		return new And(wrap);
	}

	/**
	 * A shortcut for new Simple(state)
	 *
	 * @param state The state to create the expression for
	 * @return The state expression matching the given state
	 */
	public static Simple forState(QuickState state) {
		return new Simple(state);
	}
}

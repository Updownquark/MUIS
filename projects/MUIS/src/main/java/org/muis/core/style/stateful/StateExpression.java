package org.muis.core.style.stateful;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.muis.core.mgr.MuisState;

/** An expression that can be evaluated on the {@link org.muis.core.mgr.StateEngine state} of an element */
public abstract class StateExpression implements org.muis.core.style.StyleExpression<StateExpression>, Comparable<StateExpression> {
	/** A state expression that depends on any number of other expressions */
	public static abstract class StateCollectionExpression extends StateExpression implements Iterable<StateExpression> {
		private final StateExpression [] theWrapped;

		/** @param wrapped The state expressions that this expression uses to evaluate */
		protected StateCollectionExpression(StateExpression... wrapped) {
			theWrapped = new StateExpression[wrapped.length];
			System.arraycopy(wrapped, 0, theWrapped, 0, wrapped.length);
		}

		/** @return The number of children in this state expression collection */
		public int getChildCount() {
			return theWrapped.length;
		}

		/**
		 * @param index The index of the child to get
		 * @return The child in this state expression collection at the given index
		 */
		public StateExpression getChild(int index) {
			return theWrapped[index];
		}

		@Override
		public java.util.Iterator<StateExpression> iterator() {
			return prisms.util.ArrayUtils.iterator(theWrapped, true);
		}

		@Override
		public int getPriority() {
			int ret = 1;
			for(StateExpression exp : this)
				ret += exp.getPriority();
			return ret;
		}

		/** @return This collection's set of unique children */
		protected StateExpression [] getWrappedChildren() {
			SortedSet<StateExpression> ret = new TreeSet<>();
			for(StateExpression wrapped : theWrapped) {
				add(wrapped, ret);
			}
			return ret.toArray(new StateExpression[ret.size()]);
		}

		private void add(StateExpression expr, SortedSet<StateExpression> ret) {
			if(expr.getClass() == getClass()) {
				for(StateExpression sub : ((StateCollectionExpression) expr).theWrapped)
					add(sub, ret);
			} else {
				expr = expr.getUnique();
				if(expr == null)
					return;
				else
					ret.add(expr);
			}
		}

		@Override
		public int compareTo(StateExpression expr) {
			if(!(expr instanceof StateCollectionExpression))
				return 1;
			StateCollectionExpression sce = (StateCollectionExpression) expr;
			for(int i = 0; i < theWrapped.length && i < sce.theWrapped.length; i++) {
				int ret = theWrapped[i].compareTo(sce.theWrapped[i]);
				if(ret != 0)
					return ret;
			}
			if(theWrapped.length < sce.theWrapped.length)
				return -1;
			else if(theWrapped.length > sce.theWrapped.length)
				return 1;
			else
				return 0;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && getClass() == o.getClass()
				&& prisms.util.ArrayUtils.equals(theWrapped, ((StateCollectionExpression) o).theWrapped);
		}

		@Override
		public int hashCode() {
			int ret = 13;
			for(StateExpression expr : this) {
				ret = ret * 13 + expr.hashCode();
			}
			return ret;
		}
	}

	/** An expression that is true if and only if every one of a set of expressions are true */
	public static class And extends StateCollectionExpression {
		/** @param wrapped The state expressions that this expression will use to evaluate */
		public And(StateExpression... wrapped) {
			super(wrapped);
		}

		@Override
		public boolean matches(MuisState... states) {
			for(StateExpression exp : this)
				if(!exp.matches(states))
					return false;
			return true;
		}

		@Override
		public StateExpression getUnique() {
			StateExpression [] wrapped = getWrappedChildren();
			if(wrapped.length == 0)
				return null;
			else if(wrapped.length == 1)
				return wrapped[0];
			ArrayList<Or> ors = new ArrayList<>();
			ArrayList<StateExpression> ands = new ArrayList<>();
			for(StateExpression wrap : wrapped)
				add(wrap, ors, ands);
			if(ors.isEmpty()) {
				int i;
				boolean equal = true;
				java.util.Iterator<StateExpression> iter = iterator();
				for(i = 0; i < wrapped.length && iter.hasNext(); i++)
					if(!iter.next().equals(wrapped[i])) {
						equal = false;
						break;
					}
				if(equal)
					return this;
				return new And(wrapped);
			}
			// Bubble up the ors to the top level
			int [] orIdxes = new int[ors.size()];
			SortedSet<And> retCh = new TreeSet<>();
			SortedSet<StateExpression> andCh = new TreeSet<>();
			while(true) {
				andCh.clear();
				for(int i = 0; i < ors.size(); i++)
					andCh.add(ors.get(i).getChild(orIdxes[i]));
				for(StateExpression and : ands)
					andCh.add(and.getUnique());
				retCh.add(new And(andCh.toArray(new StateExpression[andCh.size()])));
				int orIdx = orIdxes.length - 1;
				do {
					orIdxes[orIdx]++;
					if(orIdxes[orIdx] == ors.get(orIdx).getChildCount()) {
						orIdxes[orIdx] = 0;
						orIdx--;
					}
				} while(orIdx >= 0);
				if(orIdx < 0)
					break;
			}
			return new Or(retCh.toArray(new StateExpression[retCh.size()]));
		}

		private void add(StateExpression expr, ArrayList<Or> ors, ArrayList<StateExpression> ands) {
			if(expr instanceof Or)
				ors.add((Or) expr);
			else
				ands.add(expr);
		}

		@Override
		public int getWhenFalse(StateExpression expr) {
			boolean allTrue = true;
			for(StateExpression child : this) {
				int check = child.getWhenFalse(expr);
				if(check < 0)
					return -1;
				else if(check == 0)
					allTrue = false;
			}
			if(allTrue)
				return 1;
			return 0;
		}

		@Override
		public int getWhenTrue(StateExpression expr) {
			boolean allTrue = true;
			for(StateExpression child : this) {
				int check = child.getWhenTrue(expr);
				if(check < 0)
					return -1;
				else if(check == 0)
					allTrue = false;
			}
			if(allTrue)
				return 1;
			return 0;
		}

		@Override
		public int compareTo(StateExpression expr) {
			if(expr instanceof Simple || expr instanceof Not || expr instanceof Or)
				return 1;
			return super.compareTo(expr);
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append('(');
			for(int i = 0; i < getChildCount(); i++) {
				if(i > 0)
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
		public Or(StateExpression... wrapped) {
			super(wrapped);
		}

		@Override
		public boolean matches(MuisState... states) {
			for(StateExpression exp : this)
				if(exp.matches(states))
					return true;
			return false;
		}

		@Override
		public StateExpression getUnique() {
			StateExpression [] wrapped = getWrappedChildren();
			if(wrapped.length == 0)
				return null;
			else if(wrapped.length == 1)
				return wrapped[0];
			int i;
			boolean equal = true;
			java.util.Iterator<StateExpression> iter = iterator();
			for(i = 0; i < wrapped.length && iter.hasNext(); i++)
				if(!iter.next().equals(wrapped[i])) {
					equal = false;
					break;
				}
			if(equal)
				return this;
			return new Or(wrapped);
		}

		@Override
		public int getWhenFalse(StateExpression expr) {
			boolean allFalse = true;
			for(StateExpression child : this) {
				int check = child.getWhenFalse(expr);
				if(check > 0)
					return 1;
				else if(check == 0)
					allFalse = false;
			}
			if(allFalse)
				return -1;
			return 0;
		}

		@Override
		public int getWhenTrue(StateExpression expr) {
			boolean allFalse = true;
			for(StateExpression child : this) {
				int check = child.getWhenTrue(expr);
				if(check > 0)
					return 1;
				else if(check == 0)
					allFalse = false;
			}
			if(allFalse)
				return -1;
			return 0;
		}

		@Override
		public int compareTo(StateExpression expr) {
			if(expr instanceof Simple || expr instanceof Not)
				return 1;
			else if(!(expr instanceof Or))
				return -1;
			return super.compareTo(expr);
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append('(');
			for(int i = 0; i < getChildCount(); i++) {
				if(i > 0)
					ret.append(" | ");
				ret.append(getChild(i));
			}
			ret.append(')');
			return ret.toString();
		}
	}

	/** A state expression that is the negation of another expression */
	public static class Not extends StateExpression {
		private final StateExpression theWrapped;

		/** @param exp The expression to negate */
		public Not(StateExpression exp) {
			theWrapped = exp;
		}

		/** @return The state expression that this expression is a negation of */
		public StateExpression getWrapped() {
			return theWrapped;
		}

		@Override
		public boolean matches(MuisState... states) {
			return !theWrapped.matches(states);
		}

		@Override
		public Not getUnique() {
			StateExpression ret = theWrapped.getUnique();
			if(ret == theWrapped)
				return this;
			else if(ret == null)
				return null;
			else
				return ret.not();
		}

		@Override
		public int getWhenFalse(StateExpression expr) {
			return -theWrapped.getWhenFalse(expr);
		}

		@Override
		public int getWhenTrue(StateExpression expr) {
			return -theWrapped.getWhenTrue(expr);
		}

		@Override
		public int getPriority() {
			return theWrapped.getPriority() + 1;
		}

		@Override
		public int compareTo(StateExpression o) {
			if(o instanceof Simple) {
				return 1;
			} else if(!(o instanceof Not))
				return -1;
			return theWrapped.compareTo(((Not) o).theWrapped);
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
	public static class Simple extends StateExpression {
		private final MuisState theState;

		/** @param state The state to evaluate on */
		public Simple(MuisState state) {
			theState = state;
		}

		@Override
		public boolean matches(MuisState... states) {
			return prisms.util.ArrayUtils.contains(states, theState);
		}

		@Override
		public Simple getUnique() {
			return this;
		}

		@Override
		public int getWhenFalse(StateExpression expr) {
			if(expr instanceof Simple)
				return ((Simple) expr).theState.equals(theState) ? -1 : 0;
			else
				return -expr.getWhenTrue(this);
		}

		@Override
		public int getWhenTrue(StateExpression expr) {
			if(expr instanceof Simple)
				return ((Simple) expr).theState.equals(theState) ? 1 : 0;
			else
				return -expr.getWhenFalse(this);
		}

		@Override
		public int getPriority() {
			return theState.getPriority();
		}

		@Override
		public int compareTo(StateExpression expr) {
			if(!(expr instanceof Simple))
				return -1;
			return theState.compareTo(((Simple) expr).theState);
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

	private StateExpression() {
	}

	/**
	 * @param states The state of the engine to evaluate against
	 * @return Whether this expression returns true for the given state set
	 */
	public abstract boolean matches(MuisState... states);

	/**
	 * @return A state expression that is always {@link #equals(Object) equal} to any state expression that returns the same values from
	 *         {@link #matches(MuisState...)} for every possible set of states
	 */
	public abstract StateExpression getUnique();

	/** @return An expression that is the logical opposite of this expression */
	public Not not() {
		return new Not(this);
	}

	/**
	 * @param exprs The expressions to OR with this expression
	 * @return An expression that is a logical OR of this expression and the given expressions
	 */
	public Or or(StateExpression... exprs) {
		StateExpression [] wrap = new StateExpression[exprs.length + 1];
		wrap[0] = this;
		System.arraycopy(exprs, 0, wrap, 1, exprs.length);
		return new Or(wrap);
	}

	/**
	 * @param exprs The expressions to and with this expression
	 * @return An expression that is a logical AND of this expression and the given expressions
	 */
	public And and(StateExpression... exprs) {
		StateExpression [] wrap = new StateExpression[exprs.length + 1];
		wrap[0] = this;
		System.arraycopy(exprs, 0, wrap, 1, exprs.length);
		return new And(wrap);
	}
}

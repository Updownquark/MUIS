package org.muis.core.style;

import org.muis.core.mgr.MuisState;

/** An expression that can be evaluated on the {@link org.muis.core.mgr.StateEngine state} of an element */
public abstract class StateExpression {
	/** A state expression that depends on any number of other expressions */
	public static abstract class StateCollectionExpression extends StateExpression implements Iterable<StateExpression> {
		private final StateExpression [] theWrapped;

		/** @param wrapped The state expressions that this expression uses to evaluate */
		protected StateCollectionExpression(StateExpression... wrapped) {
			theWrapped = wrapped;
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
		public int getPriority() {
			return theWrapped.getPriority() + 1;
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
		public int getPriority() {
			return theState.getPriority();
		}
	}

	/**
	 * @param states The state of the engine to evaluate against
	 * @return Whether this expression returns true for the given state set
	 */
	public abstract boolean matches(MuisState... states);

	/** @return The overall priority of this expression */
	public abstract int getPriority();

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

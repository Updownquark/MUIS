package org.muis.core.style;

/** An expression that can be evaluated on the {@link org.muis.core.mgr.StateEngine state} of an element */
public abstract class StateExpression {
	public static abstract class StateCollectionExpression extends StateExpression implements Iterable<StateExpression> {
		private final StateExpression [] theWrapped;

		protected StateCollectionExpression(StateExpression... wrapped) {
			theWrapped = wrapped;
		}

		@Override
		public java.util.Iterator<StateExpression> iterator() {
			return prisms.util.ArrayUtils.iterator(theWrapped, true);
		}

		@Override
		public int getComplexity() {
			int ret = 1;
			for(StateExpression exp : this)
				ret += exp.getComplexity();
			return ret;
		}
	}

	public static class And extends StateCollectionExpression {
		public And(StateExpression... wrapped) {
			super(wrapped);
		}

		@Override
		public boolean matches(String... states) {
			for(StateExpression exp : this)
				if(!exp.matches(states))
					return false;
			return true;
		}
	}

	public static class Or extends StateCollectionExpression {
		public Or(StateExpression... wrapped) {
			super(wrapped);
		}

		@Override
		public boolean matches(String... states) {
			for(StateExpression exp : this)
				if(exp.matches(states))
					return true;
			return false;
		}
	}

	public static class Not extends StateExpression {
		private final StateExpression theWrapped;

		public Not(StateExpression exp) {
			theWrapped = exp;
		}

		public StateExpression getWrapped() {
			return theWrapped;
		}

		@Override
		public boolean matches(String... states) {
			return !theWrapped.matches(states);
		}

		@Override
		public int getComplexity() {
			return theWrapped.getComplexity() + 1;
		}
	}

	public static class Simple extends StateExpression {
		private final String theState;

		public Simple(String state) {
			theState = state;
		}

		@Override
		public boolean matches(String... states) {
			return prisms.util.ArrayUtils.contains(states, theState);
		}

		@Override
		public int getComplexity() {
			return 1;
		}
	}

	/**
	 * @param states The state of the engine to evaluate against
	 * @return Whether this expression returns true for the given state set
	 */
	public abstract boolean matches(String... states);

	/** @return The complexity of this expression */
	public abstract int getComplexity();

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

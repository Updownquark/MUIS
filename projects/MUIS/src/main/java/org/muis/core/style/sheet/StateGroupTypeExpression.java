package org.muis.core.style.sheet;

import org.muis.core.style.stateful.StateExpression;

import prisms.util.ArrayUtils;

/**
 * The expression type for style sheets, allowing style attribute values to be applied in potentially state-, group-, and type-dependent
 * ways.
 *
 * @param <E> The element type of this expression
 */
public class StateGroupTypeExpression<E extends org.muis.core.MuisElement> implements
	org.muis.core.style.StyleExpression<StateGroupTypeExpression<?>> {
	private final StateExpression theState;

	private final String theGroupName;

	private final Class<E> theType;

	/**
	 * @param state The state expression of this condition, or null if this expression is to be state-independent.
	 * @param group The group name of this condition, or null if this expression is to be group-independent.
	 * @param type The element type of this condition, or null if this expression is to be type-independent.
	 */
	public StateGroupTypeExpression(StateExpression state, String group, Class<E> type) {
		theState = state;
		theGroupName = group;
		theType = type;
	}

	@Override
	public int getWhenTrue(StateGroupTypeExpression<?> expr) {
		if(!(ArrayUtils.equals(expr.theGroupName, theGroupName) && expr.theType.isAssignableFrom(theType)))
			return 0;
		if(theState == null)
			return 1;
		if(expr.theState == null)
			return 0;
		return theState.getWhenTrue(expr.theState);
	}

	@Override
	public int getWhenFalse(StateGroupTypeExpression<?> expr) {
		if(!(ArrayUtils.equals(expr.theGroupName, theGroupName) && expr.theType.isAssignableFrom(theType)))
			return 0;
		if(theState == null)
			return 1;
		if(expr.theState == null)
			return -1;
		return theState.getWhenFalse(expr.theState);
	}

	/**
	 * Prioritizes expressions by type, state, and group, in that order
	 * 
	 * @see org.muis.core.style.StyleExpression#getPriority()
	 */
	@Override
	public int getPriority() {
		int ret = theType != null ? 1000 : 0;
		if(theState != null)
			ret += theState.getPriority();
		if(theGroupName != null)
			ret += 1;
		return ret;
	}

	/** @return The state expression of this condition, or null if this expression is state-independent. */
	public StateExpression getState() {
		return theState;
	}

	/** @return The group name of this condition, or null if this expression is group-independent. */
	public String getGroupName() {
		return theGroupName;
	}

	/** @return The element type of this condition, or null if this expression is type-independent. */
	public Class<E> getType() {
		return theType;
	}
}

package org.quick.core.style.stateful;

import prisms.lang.Type;

/**
 * An extension of QuickStyle that may have different attribute settings depending on a state. QuickStyle query methods on an implementation
 * of this class return the value for the element's current state, if it has one, or the base state (where no states are active) otherwise.
 */
public interface StatefulStyle extends org.quick.core.style.ConditionalStyle<StatefulStyle, StateExpression> {
	@Override
	public default Type getExpressionType() {
		return new Type(StateExpression.class);
	}
}

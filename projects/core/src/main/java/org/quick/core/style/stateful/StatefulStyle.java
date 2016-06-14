package org.quick.core.style.stateful;

import com.google.common.reflect.TypeToken;

/**
 * An extension of QuickStyle that may have different attribute settings depending on a state. QuickStyle query methods on an implementation
 * of this class return the value for the element's current state, if it has one, or the base state (where no states are active) otherwise.
 */
public interface StatefulStyle extends org.quick.core.style.ConditionalStyle<StatefulStyle, StateExpression> {
	@Override
	default TypeToken<StateExpression> getExpressionType() {
		return TypeToken.of(StateExpression.class);
	}
}

package org.quick.core.style.sheet;

import com.google.common.reflect.TypeToken;

/**
 * Represents a style sheet in Quick that can be populated with style attribute values that are potentially specific to a
 * {@link org.quick.core.style.attach.NamedStyleGroup group}, {@link org.quick.core.style.attach.TypedStyleGroup type}, and/or
 * {@link org.quick.core.mgr.QuickState state}
 */
public interface StyleSheet extends org.quick.core.style.ConditionalStyle<StyleSheet, StateGroupTypeExpression<?>> {
	@Override
	public default TypeToken<StateGroupTypeExpression<?>> getExpressionType() {
		return new TypeToken<StateGroupTypeExpression<?>>() {};
	}
}

package org.quick.core.style2;

import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleExpression;

import com.google.common.reflect.TypeToken;

public interface ConditionalStyle<C extends ConditionalStyle<C, E>, E extends StyleExpression<E>> {
	/** @return The type of expressions that this conditional style uses */
	TypeToken<E> getExpressionType();

	/**
	 * @return The styles that this style depends on for attribute values when an attribute's value is not set in this style directly for a
	 *         state
	 */
	ObservableList<C> getConditionalDependencies();

	/** @return All style attributes that are set for any condition in this style specifically */
	ObservableSet<StyleAttribute<?>> localAttributes();

	/** @return All style attributes that are set for any condition in this style or any of its dependencies */
	ObservableSet<StyleAttribute<?>> attributes();
}
